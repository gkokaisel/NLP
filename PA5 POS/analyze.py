import sys
from collections import defaultdict

def read_vocab():
    POS = {}
    with file("Vocab.gr") as f:
        for line in f:
            line = line.strip()
            if not line or line[0]=='#': continue
            parts = line.split('\t')
            if len(parts) != 3: continue
            POS[parts[2]] = parts[1]
    return POS

def analyze(fname, POS):
    POS_pairs = defaultdict(lambda:0)
    with file(fname) as f:
        for line in f:
            line = line.strip()
            if not line: continue
            tokens = line.split(' ')
            # repack tokens into words (can be complex words like "Round Table")
            n = 0
            words = []
            ntokens = len(tokens)
            while n < ntokens:
                word = tokens[n]
                while not POS.get(word) and n < ntokens - 1:
                    n += 1
                    word += ' ' + tokens[n]
                n += 1
                words.append(word)
            POS_pairs[('', POS[words[0]])] += 1
            POS_pairs[(POS[words[-1]], '')] += 1
            for n in range(len(words)-1):
                POS_pairs[(POS[words[n]], POS[words[n+1]])] += 1
    return POS_pairs