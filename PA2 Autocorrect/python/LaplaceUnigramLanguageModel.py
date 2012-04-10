import math, collections

class LaplaceUnigramLanguageModel:

	def __init__(self, corpus):
		"""Initialize your data structures in the constructor."""
		self.unigramCounts = {}
		self.train(corpus)

	def train(self, corpus):
		""" Takes a corpus and trains your language model. 
		Compute any counts or other corpus statistics in this function.
		"""  
		for sentence in corpus.corpus:
			for datum in sentence.data:
				token = datum.word
				if token in self.unigramCounts:
					self.unigramCounts[token] = self.unigramCounts[token] + 1.0
				else:
					self.unigramCounts[token] = 1.0

	def score(self, sentence):
		""" Takes a list of strings as argument and returns the log-probability of the 
		sentence using your language model. Use whatever data you computed in train() here.
		"""
		score = 1.0
		vocabulary = len(self.unigramCounts) + 0.0
		for token in sentence:
			token_find = self.unigramCounts[token] if token in self.unigramCounts else 0.0			
			score += math.log(token_find + 1.0)
			score -= math.log(token_find + vocabulary)
		return score
		
