package com.horizon.home;

import android.content.Context;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * Talks to the projector's own network-debugging port (127.0.0.1:5555) so Home can run a shell
 * command like "dumpsys meminfo" with shell rights, without a PC. The first time, the projector
 * shows "Allow network debugging?" once; choose Always allow with the remote.
 */
final class AdbLocal {
    private static final int CNXN = 0x4e584e43, AUTH = 0x48545541, OPEN = 0x4e45504f,
            OKAY = 0x59414b4f, CLSE = 0x45534c43, WRTE = 0x45545257;
    private static final byte[] SHA1_PREFIX = {0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e, 0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14};

    /** Result of the last attempt, for the UI. */
    static volatile String state = "idle"; // idle, waiting (approval on screen), ok, off (port closed), denied

    static synchronized String exec(Context c, String cmd) {
        Socket s = new Socket();
        try {
            s.connect(new InetSocketAddress("127.0.0.1", 5555), 1500);
            s.setSoTimeout(10000);
            DataInputStream in = new DataInputStream(s.getInputStream());
            OutputStream out = s.getOutputStream();
            KeyPair kp = keys(c);
            send(out, CNXN, 0x01000001, 256 * 1024, "host::\0".getBytes(StandardCharsets.US_ASCII));
            boolean sentSig = false, sentKey = false;
            while (true) {
                Msg m = read(in);
                if (m.cmd == CNXN) break;
                if (m.cmd != AUTH || m.arg0 != 1) continue;
                if (!sentSig) {
                    Signature sig = Signature.getInstance("NONEwithRSA");
                    sig.initSign(kp.getPrivate());
                    sig.update(SHA1_PREFIX);
                    sig.update(m.data);
                    send(out, AUTH, 2, 0, sig.sign());
                    sentSig = true;
                } else if (!sentKey) {
                    state = "waiting";
                    send(out, AUTH, 3, 0, (pubKey((RSAPublicKey) kp.getPublic()) + " horizon@home\0").getBytes(StandardCharsets.US_ASCII));
                    sentKey = true;
                    s.setSoTimeout(60000); // waiting for "Always allow" on screen
                } else {
                    state = "denied";
                    return null;
                }
            }
            s.setSoTimeout(15000);
            send(out, OPEN, 1, 0, ("shell:" + cmd + "\0").getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            int remote = 0;
            while (true) {
                Msg m = read(in);
                if (m.cmd == OKAY) { remote = m.arg0; continue; }
                if (m.cmd == WRTE) {
                    buf.write(m.data);
                    send(out, OKAY, 1, m.arg0, new byte[0]);
                    continue;
                }
                if (m.cmd == CLSE) { send(out, CLSE, 1, remote, new byte[0]); break; }
            }
            state = "ok";
            return buf.toString("UTF-8");
        } catch (java.net.ConnectException e) {
            state = "off";
            return null;
        } catch (java.net.SocketTimeoutException e) {
            if (!"waiting".equals(state)) state = "off";
            return null;
        } catch (Throwable t) {
            if (!"waiting".equals(state)) state = "off";
            return null;
        } finally {
            try { s.close(); } catch (Throwable ignored) {}
        }
    }

    // ---------------------------------------------------------------- wire

    private static final class Msg { int cmd, arg0, arg1; byte[] data; }

    private static void send(OutputStream out, int cmd, int a0, int a1, byte[] data) throws Exception {
        ByteBuffer b = ByteBuffer.allocate(24 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        int sum = 0;
        for (byte x : data) sum += x & 0xff;
        b.putInt(cmd).putInt(a0).putInt(a1).putInt(data.length).putInt(sum).putInt(~cmd).put(data);
        out.write(b.array());
        out.flush();
    }

    private static Msg read(DataInputStream in) throws Exception {
        byte[] h = new byte[24];
        in.readFully(h);
        ByteBuffer b = ByteBuffer.wrap(h).order(ByteOrder.LITTLE_ENDIAN);
        Msg m = new Msg();
        m.cmd = b.getInt();
        m.arg0 = b.getInt();
        m.arg1 = b.getInt();
        int len = b.getInt();
        m.data = new byte[len];
        in.readFully(m.data);
        return m;
    }

    // ---------------------------------------------------------------- keys

    private static KeyPair keys(Context c) throws Exception {
        File pri = new File(c.getFilesDir(), "adbkey"), pub = new File(c.getFilesDir(), "adbkey.pub");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        if (pri.exists() && pub.exists()) {
            PrivateKey p = kf.generatePrivate(new PKCS8EncodedKeySpec(readAll(pri)));
            java.security.PublicKey q = kf.generatePublic(new X509EncodedKeySpec(readAll(pub)));
            return new KeyPair(q, p);
        }
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair kp = g.generateKeyPair();
        writeAll(pri, kp.getPrivate().getEncoded());
        writeAll(pub, kp.getPublic().getEncoded());
        return kp;
    }

    /** Android's adb public key format: words, n0inv, modulus (LE), rr (LE), exponent; base64. */
    private static String pubKey(RSAPublicKey k) {
        BigInteger n = k.getModulus();
        int words = 64;
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = n.mod(r32).modInverse(r32).negate().mod(r32);
        BigInteger rr = BigInteger.ONE.shiftLeft(words * 32 * 2).mod(n);
        ByteBuffer b = ByteBuffer.allocate(4 + 4 + words * 4 + words * 4 + 4).order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(words).putInt(n0inv.intValue());
        putLE(b, n, words);
        putLE(b, rr, words);
        b.putInt(k.getPublicExponent().intValue());
        return Base64.encodeToString(b.array(), Base64.NO_WRAP);
    }

    private static void putLE(ByteBuffer b, BigInteger v, int words) {
        BigInteger mask = BigInteger.ONE.shiftLeft(32).subtract(BigInteger.ONE);
        for (int i = 0; i < words; i++) {
            b.putInt(v.shiftRight(i * 32).and(mask).intValue());
        }
    }

    private static byte[] readAll(File f) throws Exception {
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] d = new byte[(int) f.length()];
            new DataInputStream(in).readFully(d);
            return d;
        }
    }

    private static void writeAll(File f, byte[] d) throws Exception {
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(d); }
    }
}
