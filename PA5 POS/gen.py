import sys
import analyze

def main(args):
    POS = analyze.read_vocab()
    POS_pairs = analyze.analyze("dev.sen", POS)
    symdict = {}
    for pos in POS.values():
        symdict[pos] = 1
    syms = symdict.keys()
    syms.sort()
    sys.stdout.write("1\tS2\n")
    for sym in syms:
        count = POS_pairs[('',sym)] + 1
        sys.stdout.write("%d\tS2\t_%s\n" % (count, sym))
    for sym in syms:
        count = POS_pairs[(sym, '')] + 1
        sys.stdout.write("%d\t_%s\t%s\n" % (count, sym, sym))
        for sym1 in syms:
            count = POS_pairs[(sym, sym1)] + 1
            sys.stdout.write("%d\t_%s\t%s _%s\n" % (count, sym, sym, sym1))
    return 0

def read_syms(fname):
    syms = []
    with file(args[0]) as f:
        for line in f:
            line = line.strip()
            if not line: continue
            syms.append(line)
    return syms

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))