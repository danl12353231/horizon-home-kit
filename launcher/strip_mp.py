"""Remove MethodParameters attributes (javac 21 writes nameless ones that old d8 can't read)."""
import struct, sys, pathlib

def strip(path):
    b = path.read_bytes()
    pos = 8
    n = struct.unpack('>H', b[pos:pos+2])[0]; pos += 2
    utf = {}
    i = 1
    while i < n:
        tag = b[pos]; pos += 1
        if tag == 1:
            l = struct.unpack('>H', b[pos:pos+2])[0]
            utf[i] = b[pos+2:pos+2+l].decode('utf-8', 'replace'); pos += 2 + l
        elif tag in (3, 4): pos += 4
        elif tag in (5, 6): pos += 8; i += 1
        elif tag in (7, 8, 16, 19, 20): pos += 2
        elif tag in (9, 10, 11, 12, 17, 18): pos += 4
        elif tag == 15: pos += 3
        else: raise ValueError('bad tag %d in %s' % (tag, path))
        i += 1
    out = bytearray(b[:pos])
    out += b[pos:pos+6]; pos += 6
    ic = struct.unpack('>H', b[pos:pos+2])[0]
    out += b[pos:pos+2+2*ic]; pos += 2 + 2*ic
    removed = 0
    for _ in range(2):  # fields, methods
        cnt = struct.unpack('>H', b[pos:pos+2])[0]
        out += b[pos:pos+2]; pos += 2
        for _ in range(cnt):
            out += b[pos:pos+6]; pos += 6
            ac = struct.unpack('>H', b[pos:pos+2])[0]; pos += 2
            keep = []
            for _ in range(ac):
                ni, ln = struct.unpack('>HI', b[pos:pos+6])
                chunk = b[pos:pos+6+ln]; pos += 6 + ln
                if utf.get(ni) == 'MethodParameters':
                    removed += 1
                    continue
                keep.append(chunk)
            out += struct.pack('>H', len(keep))
            for c in keep: out += c
    out += b[pos:]
    if removed:
        path.write_bytes(bytes(out))
    return removed

total = 0
for p in pathlib.Path(sys.argv[1]).rglob('*.class'):
    total += strip(p)
print('stripped', total, 'MethodParameters attributes')
