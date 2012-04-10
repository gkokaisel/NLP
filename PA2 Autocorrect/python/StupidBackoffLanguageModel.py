import math, collections

class StupidBackoffLanguageModel:

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
			preceding_token = ""
			for datum in sentence.data:
				token = datum.word
				if token in self.unigramCounts:
					self.unigramCounts[token] = self.unigramCounts[token] + 1.0
				else:
					self.unigramCounts[token] = 1.0
				if preceding_token != "":
					bigram = preceding_token + " | " + token
					if bigram in self.bigramCounts:
						self.bigramCounts[bigram] = self.bigramCounts[bigram] + 1.0
					else:
						self.bigramCounts[bigram] = 1.0
				preceding_token = token
          
	def score(self, sentence):
		""" Takes a list of strings as argument and returns the log-probability of the 
		sentence using your language model. Use whatever data you computed in train() here.
		"""
		score = 1.0
		unigram_vocabulary = len(self.unigramCounts) + 0.0
		preceding_token = ""
		for token in sentence:
			unigram_find = self.unigramCounts[token] if token in self.unigramCounts else 0.0
			bigram = preceding_token + " | " + token
			bigram_find = self.bigramCounts[bigram] if bigram in self.bigramCounts else 0.0
			preceding_token_find = self.unigramCounts[preceding_token] if preceding_token in self.unigramCounts else 0.0			
			if bigram_find == 0.0 or preceding_token_find == 0.0:				
                                score += math.log(0.4 * (unigram_find + 1.0))
                                score -= math.log(unigram_find + unigram_vocabulary)                               
                        else:				
                                score += math.log(bigram_find)
                                score -= math.log(preceding_token_find)                        
                        preceding_token = token
		return score
