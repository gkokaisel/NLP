import math, collections

class CustomLanguageModel:

	def __init__(self, corpus):
		"""Initialize your data structures in the constructor."""
		self.unigramCounts = {}
		self.bigramCounts = {}
		self.trigramCounts = {}
		self.quadgramCounts = {}	
		self.train(corpus)

	def train(self, corpus):
		""" Takes a corpus and trains your language model. 
		Compute any counts or other corpus statistics in this function.
		"""  
		for sentence in corpus.corpus:
			preceding_token = ""
			two_back_token = ""
			three_back_token = ""
			four_back_token = ""
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
						
				if two_back_token != "":
					trigram = two_back_token + " | " + preceding_token + " | " + token
					if trigram in self.trigramCounts:
						self.trigramCounts[trigram] = self.trigramCounts[trigram] + 1.0
					else:
						self.trigramCounts[trigram] = 1.0

				if three_back_token != "":
					quadgram = three_back_token + " | " + two_back_token + " | " + preceding_token + " | " + token
					if quadgram in self.quadgramCounts:
						self.quadgramCounts[quadgram] = self.quadgramCounts[quadgram] + 1.0
					else:
						self.quadgramCounts[quadgram] = 1.0
					
				four_back_token = three_back_token		
				three_back_token = two_back_token		
				two_back_token = preceding_token
				preceding_token = token
        
	def score(self, sentence):
		""" Takes a list of strings as argument and returns the log-probability of the 
		sentence using your language model. Use whatever data you computed in train() here.
		"""
		score = 1.0
		unigram_vocabulary = len(self.unigramCounts) + 0.0
		preceding_token = ""
		two_back_token = ""
		three_back_token = ""
		four_back_token = ""
		for token in sentence:
			unigram_find = self.unigramCounts[token] if token in self.unigramCounts else 0.0
			bigram = preceding_token + " | " + token
			bigram_find = self.bigramCounts[bigram] if bigram in self.bigramCounts else 0.0
			trigram = two_back_token + " | " + preceding_token + " | " + token
			trigram_find = self.trigramCounts[trigram] if trigram in self.trigramCounts else 0.0
			quadgram = three_back_token + " | " + two_back_token + " | " + preceding_token + " | " + token
			quadgram_find = self.quadgramCounts[quadgram] if quadgram in self.quadgramCounts else 0.0
			preceding_token_find = self.unigramCounts[preceding_token] if preceding_token in self.unigramCounts else 0.0
			preceding_bigram = two_back_token + " | " + preceding_token			
			preceding_bigram_find = self.bigramCounts[preceding_bigram] if preceding_bigram in self.bigramCounts else 0.0
			preceding_trigram = three_back_token + " | " + two_back_token
			preceding_trigram_find = self.trigramCounts[preceding_trigram] if preceding_trigram in self.trigramCounts else 0.0
                        # If we can't find a good quadgram
                        if quadgram_find == 0.0 or preceding_trigram_find == 0.0:
                                # If we can't find a good trigram
                                if trigram_find == 0.0 or preceding_bigram_find == 0.0:
                                        # If we can't find a good bigram
                                        if bigram_find == 0.0 or preceding_token_find == 0.0:                                        
                                                score += math.log(0.4 * (unigram_find + 1.0))
                                                score -= math.log(unigram_find + unigram_vocabulary)
                                        else:                                        
                                                score += math.log(bigram_find)
                                                score -= math.log(preceding_token_find)
                                else:                                
                                        score += math.log(trigram_find)
                                        score -= math.log(preceding_bigram_find)
                        else:
                                score += math.log(quadgram_find)                                
                                score -= math.log(preceding_trigram_find)
                                

                        four_back_token = three_back_token
                        three_back_token = two_back_token
                        two_back_token = preceding_token
                        preceding_token = token
        	if unigram_find > 0:
				#print len(self.unigramCounts)
			
			
			return score
		
		"""
		notes:
		trigram model http://en.wikipedia.org/wiki/Trigram		
		"""
