import math, collections

class LaplaceBigramLanguageModel:

	def __init__(self, corpus):
		"""Initialize your data structures in the constructor."""
		self.unigramCounts = {}
		self.bigramCounts = {}
		self.train(corpus)

	def train(self, corpus):
		""" Takes a corpus and trains your language model. 
		Compute any counts or other corpus statistics in this function.
		"""  
		for sentence in corpus.corpus:
			previous_token = ""
			for datum in sentence.data:
				token = datum.word
				if token in self.unigramCounts:
					self.unigramCounts[token] = self.unigramCounts[token] + 1.0
				else:
					self.unigramCounts[token] = 1.0
				if previous_token != "":
					bigram = previous_token + " | " + token								
					if bigram in self.bigramCounts:
						self.bigramCounts[bigram] = self.bigramCounts[bigram] + 1.0
					else:
						self.bigramCounts[bigram] = 1.0
				previous_token = token
		
	def score(self, sentence):
		""" Takes a list of strings as argument and returns the log-probability of the 
		sentence using your language model. Use whatever data you computed in train() here.
		"""
		score = 1.0
		vocabulary = len(self.bigramCounts) + 0.0		
		previous_token = ""
		for token in sentence:
			unigram_find = self.unigramCounts[token] if token in self.unigramCounts else 0.0
			bigram = previous_token + " | " + token
			bigram_find = self.bigramCounts[bigram] if bigram in self.bigramCounts else 0.0						
			score += math.log(bigram_find + 1.0)
			score -= math.log(unigram_find + vocabulary)
			previous_token = token
		return score
                


