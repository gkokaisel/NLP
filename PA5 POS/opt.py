# python 3
# integer weights
# doesn't change weights of START expansions
# use:
# 	opt.py S1.gr dev.sen
#	opt.py S2.gr dev.sen
# optimizing S2.gr works only if it matches anyting
# you may want ot comment out "START S1" in S1.gr

import re
import os,sys,shutil
import time

class Line:
	def twiddleUp(self):
		return False
	def twiddleDown(self):
		return False
	def keep(self):
		pass
	def backoff(self):
		pass
	def hasLowDelta(self):
		return True

class PassiveLine(Line):
	def __init__(self,text):
		self.text=text
	def __str__(self):
		return self.text

class ActiveLine(Line):
	def __init__(self,weight,text):
		self.prevWeight=self.weight=weight
		self.text=text
		self.delta=1
	def __str__(self):
		return str(self.weight)+'\t'+self.text
	def twiddleUp(self):
		if self.weight>=10000:
			return False
		self.prevWeight=self.weight
		self.weight=int(self.weight+self.delta)
		if self.weight==self.prevWeight:
			self.weight+=1
		return True
	def twiddleDown(self):
		if self.weight<=1:
			return False
		self.prevWeight=self.weight
		self.weight=int(self.weight-self.delta)
		if self.weight==self.prevWeight:
			self.weight-=1
		return True
	def keep(self):
		#self.delta*=1.1
		self.delta+=1
	def backoff(self):
		#self.delta*=0.9
		self.delta=max(0,self.delta-1)
		self.weight=self.prevWeight
	def hasLowDelta(self):
		return self.delta<=1

class Grammar:
	def __init__(self,filename):
		self.filename=filename
		self.lines=[]
		self.readFile()
		self.backupFile()
	def __str__(self):
		return '\n'.join(str(line) for line in self.lines)
	def addLine(self,line):
		match=re.match('([0-9.]+)\t(.*)',line)
		if match:
			weight=int(match.group(1))
			text=match.group(2)
			if re.match('START\t',text):
				self.lines.append(PassiveLine(line))
			else:
				self.lines.append(ActiveLine(weight,text))
		else:
			self.lines.append(PassiveLine(line))
	def readFile(self):
		file=open(self.filename)
		for line in file:
			self.addLine(line.rstrip())
		file.close()
	def writeFile(self):
		file=open(self.filename,'w')
		file.write(str(self))
		file.close()
	def backupFile(self):
		shutil.copy(self.filename,self.filename+'.'+str(int(time.time()))+'.backup')
	def runGrammar(self,senFilename):
		perplexity=None
		for line in os.popen("java -jar pcfg.jar parse "+senFilename+" *.gr"):
			match=re.search('perplexity=(.*)',line)
			if match:
				perplexity=float(match.group(1))
		return perplexity
	def twiddlePass(self,senFilename,perplexity):
		for line in self.lines:
			def tw(c):
				self.writeFile()
				p=self.runGrammar(senFilename)
				k=p<perplexity
				print('['+(c if k else '=')+' pold='+str(perplexity)+' pnew='+str(p)+']\t',line)
				if k:
					line.keep()
					return p,True
				else:
					line.backoff()
					return perplexity,False

			if line.twiddleUp():
				perplexity,flag=tw('+')
				if flag:
					continue
			if line.twiddleDown():
				perplexity,flag=tw('-')
				if flag:
					continue
		self.writeFile()
		self.backupFile()
		return perplexity
	def twiddle(self,senFilename):
		perplexity=self.runGrammar(senFilename)
		for i in range(100):
			print('pass',i+1,'perplexity',str(perplexity))
			p=self.twiddlePass(senFilename,perplexity)
			if perplexity-p<.001 and all(line.hasLowDelta() for line in self.lines):
				break
			perplexity=p

filename=sys.argv[1]
senFilename=sys.argv[2]
grammar=Grammar(filename)
grammar.twiddle(senFilename)
