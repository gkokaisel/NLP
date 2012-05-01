package nlpclass.assignments;

import nlpclass.io.MASCTreebankReader;
import nlpclass.io.PennTreebankReader;
import nlpclass.ling.Tree;
import nlpclass.ling.Trees;
import nlpclass.parser.EnglishPennTreebankParseEvaluator;
import nlpclass.util.*;

import java.util.*;


/**
 * Harness for PCFG Parser project.
 *
 * @author Dan Klein
 */
public class PCFGParserTester {
    
    private static final String ROOT_STR = "ROOT";

  // Parser interface ===========================================================

  /**
   * Parsers are required to map sentences to trees.  How a parser is
   * constructed and trained is not specified.
   */
  public static interface Parser {
    public void train(List<Tree<String>> trainTrees);
    public Tree<String> getBestParse(List<String> sentence);
  }


  // PCFGParser =================================================================

  /**
   * The PCFG Parser you will implement.
   */
  public static class PCFGParser implements Parser {
    
    private Grammar grammar;
    private Lexicon lexicon;
    private List<String> tagList;
    private List<String> currentSentence;
    
    public void train(List<Tree<String>> trainTrees) {
      // TODO: before you generate your grammar, the training trees
      // need to be binarized so that rules are at most binary
      TreeAnnotations.vertMarkovization(trainTrees);
      
      //System.out.println("First sublabel: " + trainTrees.get(0).getChildren().get(0).getLabel());
      lexicon = new Lexicon(TreeAnnotations.annotateAllTrees(trainTrees));
      grammar = new Grammar(TreeAnnotations.annotateAllTrees(trainTrees));
      tagList = new ArrayList<String>(lexicon.getAllTags());
      //bp.train(trainTrees);
    }
    
    public Tree<String> getBestParse(List<String> sentence) {
      currentSentence = sentence;
      int length = sentence.size() + 1;
      Map<String,Double>[][] LOGscoreTable = new HashMap[length][length];
      Map<String,Pair<String,String>>[][] history = new HashMap[length][length];
      Map<String,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>>[][] childLocations = new HashMap[length][length];
      for (int pos = 0; pos < length - 1; pos++) {
        LOGscoreTable[pos][pos + 1] = new HashMap<String,Double>();
        history[pos][pos + 1] = new HashMap<String,Pair<String,String>>();
        childLocations[pos][pos + 1] = new HashMap<String,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>>();
        //System.out.println("Beginning tag iteration...");
        for (String tag : tagList) {
          if (lexicon.scoreTagging(sentence.get(pos), tag) <= 0.0) {
            continue;
          }
          LOGscoreTable[pos][pos + 1].put(tag, Math.log(lexicon.scoreTagging(sentence.get(pos), tag)));
          Pair<String,String> historyPair = new Pair<String,String>(sentence.get(pos), null);
          history[pos][pos + 1].put(tag, historyPair);
          childLocations[pos][pos + 1].put(tag, null); // null denotes leaf node...
        }
        //System.out.println("Ending tag iteration!");
        boolean added;
        String toAdd = "";
        double matching = 0.0;
        do {
          added = false;
          for (String tag : LOGscoreTable[pos][pos + 1].keySet()) {
            List<UnaryRule> rules = grammar.getrulesByChild(tag);
            for (UnaryRule rule : rules) {
              double ruleProb = Math.log(rule.getScore()) + LOGscoreTable[pos][pos + 1].get(tag);
              //System.out.println("rule: " + rule);
              if ( (! LOGscoreTable[pos][pos + 1].containsKey(rule.getParent())) || ruleProb > LOGscoreTable[pos][pos + 1].get(rule.getParent())) {
                // If new rule is better than old prob in this box, replace...
                toAdd = rule.getParent();
                matching = ruleProb;
                
                Pair<String,String> newPair = new Pair<String,String>(rule.getChild(), null);
                history[pos][pos + 1].put(rule.getParent(), newPair);
                Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> toPut = new Pair(new Pair<Integer,Integer>(pos, pos + 1), null); // Second loc null, unary rule
                childLocations[pos][pos + 1].put(rule.getParent(), toPut);
                added = true;
                break;
              }
            }
            if (added) {
              break;
            }
          }
          if (added) {
            LOGscoreTable[pos][pos + 1].put(toAdd, matching);
          }
        } while (added);
      }
      
      for (int span = 2; span < length; span++) {
        for (int begin = 0; begin < length-span; begin++) {
          int end = begin + span;
          LOGscoreTable[begin][end] = new HashMap<String,Double>();
          history[begin][end] = new HashMap<String,Pair<String,String>>();
          childLocations[begin][end] = new HashMap<String,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>>();
          for (int split = begin+1; split < end; split++) {
            for (String tag : LOGscoreTable[begin][split].keySet()) {
              List<BinaryRule> binRules = grammar.getBinaryRulesByLeftChild(tag);
              for (BinaryRule bRule : binRules) {
                if (! LOGscoreTable[split][end].containsKey(bRule.getRightChild())) {
                  continue;
                }
                double rightProb = LOGscoreTable[split][end].get(bRule.getRightChild());
                double leftProb = LOGscoreTable[begin][split].get(tag);
                double newProb = leftProb + rightProb + Math.log(bRule.getScore());
                if ( (! LOGscoreTable[begin][end].containsKey(bRule.getParent())) || newProb > LOGscoreTable[begin][end].get(bRule.getParent())) {
                  LOGscoreTable[begin][end].put(bRule.getParent(), newProb);
                  Pair<String,String> stringPair = new Pair<String,String>(bRule.getLeftChild(), bRule.getRightChild());
                  history[begin][end].put(bRule.getParent(), stringPair);
                  Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> locationPair = new Pair(new Pair<Integer,Integer>(begin, split), new Pair<Integer,Integer>(split, end));
                  childLocations[begin][end].put(bRule.getParent(), locationPair);
                }
              }
            }
            
            boolean added;
            String toAdd = "";
            double matching = 0.0;
            do {
              added = false;
              for (String tag : LOGscoreTable[begin][end].keySet()) {
                List<UnaryRule> rules = grammar.getrulesByChild(tag);
                for (UnaryRule rule : rules) {
                  double ruleProb = Math.log(rule.getScore()) + LOGscoreTable[begin][end].get(tag);
                  if ( (! LOGscoreTable[begin][end].containsKey(rule.getParent())) || ruleProb > LOGscoreTable[begin][end].get(rule.getParent())) {
                    // If new rule is better than old prob in this box, replace...
                    toAdd = rule.getParent();
                    matching = ruleProb;
                    Pair<String,String> stringPair = new Pair<String,String>(rule.getChild(), null);
                    history[begin][end].put(rule.getParent(), stringPair);
                    Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> locationPair = new Pair(new Pair<Integer,Integer>(begin, end), null); // Second loc null -> unary rule
                    childLocations[begin][end].put(rule.getParent(), locationPair);
                    added = true;
                    break;
                  }
                }
                if (added) {
                  break;
                }
              }
              if (added) {
                LOGscoreTable[begin][end].put(toAdd, matching);
              }
            } while (added);
            
          }
        }
      }
      return TreeAnnotations.unAnnotateTree(this.buildTree(LOGscoreTable, history, childLocations));
    }
    
    private Tree<String> buildTree(Map<String,Double>[][] LOGscoreTable, Map<String,Pair<String,String>>[][] history,
                                   Map<String,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>>[][] childLocations) {
      int length = LOGscoreTable.length;
      Tree<String> root = recursiveBuildTREE(history, childLocations, ROOT_STR, new Pair<Integer,Integer>(0, length - 1));

      System.out.println("\nPrinting top cell...");
      for (String key : history[0][length - 1].keySet()) {
        System.out.println("\tKey: " + key);
        System.out.println("\tVal: " + history[0][length - 1].get(key));
        System.out.println("\tProb: " + LOGscoreTable[0][length - 1].get(key));
      }
      return root;
    }
    
    private Tree<String> recursiveBuildTREE(Map<String,Pair<String,String>>[][] history,
                                      Map<String,Pair<Pair<Integer,Integer>,Pair<Integer,Integer>>>[][] childLocations,
                                      String currentTAG, Pair<Integer,Integer> currentPOS) {
      Tree<String> nodeReturn = new Tree<String>(currentTAG);
      List<Tree<String>> children = new ArrayList<Tree<String>>();
      Pair<Pair<Integer,Integer>,Pair<Integer,Integer>> childlocationPair = childLocations[currentPOS.getFirst()][currentPOS.getSecond()].get(currentTAG);
      Pair<String,String> historyPair = history[currentPOS.getFirst()][currentPOS.getSecond()].get(currentTAG);
      
      if (childlocationPair == null) { // leaf tag...
        //System.out.println("historyPair: " + historyPair);
        if (historyPair != null) {
          Tree<String> leaf = new Tree<String>(historyPair.getFirst());
          children.add(leaf);
        }
        else {
          System.out.println("Null hist tag: " + currentTAG);
          System.out.println("Null hist pos: " + currentPOS);
          System.out.println("Existing keys: " + history[currentPOS.getFirst()][currentPOS.getSecond()].keySet());
          Tree<String> leaf = new Tree<String>(currentSentence.get(currentPOS.getFirst())); // Guess at word...
          children.add(leaf);
        }
      }
      else if (historyPair.getSecond() == null) {  // if tag is unary, pos stays the same...
        children.add(recursiveBuildTREE(history, childLocations, historyPair.getFirst(), currentPOS));
		
      }
      else { // binary rule...
        children.add(recursiveBuildTREE(history, childLocations, historyPair.getFirst(), childlocationPair.getFirst()));
        children.add(recursiveBuildTREE(history, childLocations, historyPair.getSecond(), childlocationPair.getSecond()));
      }
      
      nodeReturn.setChildren(children);
      return nodeReturn;
    }
  }

  // BaselineParser =============================================================

  /**
   * Baseline parser (though not a baseline I've ever seen before).  Tags the
   * sentence using the baseline tagging method, then either retrieves a known
   * parse of that tag sequence, or builds a right-branching parse for unknown
   * tag sequences.
   */
  public static class BaselineParser implements Parser {
    
    CounterMap<List<String>,Tree<String>> knownParses;
    CounterMap<Integer,String> spanToCategories;
    Lexicon lexicon;

    public void train(List<Tree<String>> trainTrees) {
      //System.out.println("Training baseline...");
      lexicon = new Lexicon(trainTrees);
      knownParses = new CounterMap<List<String>, Tree<String>>();
      spanToCategories = new CounterMap<Integer, String>();
      for (Tree<String> trainTree : trainTrees) {
        List<String> tags = trainTree.getPreTerminalYield();
        knownParses.incrementCount(tags, trainTree, 1.0);
        tallySpans(trainTree, 0);
      }
    }

    public Tree<String> getBestParse(List<String> sentence) {
      //System.out.println("GBPing baseline...");
      List<String> tags = getBaselineTagging(sentence);
      if (knownParses.keySet().contains(tags)) {
        return getBestKnownParse(tags, sentence);
      }
      return buildRightBranchParse(sentence, tags);
    }

    /* Builds a tree that branches to the right.  For pre-terminals it
     * uses the most common tag for the word in the training corpus.
     * For all other non-terminals it uses the tag that is most common
     * in training corpus of tree of the same size span as the tree
     * that is being labeled. */
    private Tree<String> buildRightBranchParse(List<String> words, List<String> tags) {
      int currentPosition = words.size() - 1;
      Tree<String> rightBranchTree = buildTagTree(words, tags, currentPosition);
      while (currentPosition > 0) {
        currentPosition--;
        rightBranchTree = merge(buildTagTree(words, tags, currentPosition),
                                rightBranchTree);
      }
      rightBranchTree = addRoot(rightBranchTree);
      return rightBranchTree;
    }

    private Tree<String> merge(Tree<String> leftTree, Tree<String> rightTree) {
      int span = leftTree.getYield().size() + rightTree.getYield().size();
      String mostFrequentLabel = spanToCategories.getCounter(span).argMax();
      List<Tree<String>> children = new ArrayList<Tree<String>>();
      children.add(leftTree);
      children.add(rightTree);
      return new Tree<String>(mostFrequentLabel, children);
    }

    private Tree<String> addRoot(Tree<String> tree) {
      return new Tree<String>(ROOT_STR, Collections.singletonList(tree));
    }

    private Tree<String> buildTagTree(List<String> words,
                                      List<String> tags,
                                      int currentPosition) {
      Tree<String> leafTree = new Tree<String>(words.get(currentPosition));
      Tree<String> tagTree = new Tree<String>(tags.get(currentPosition), 
                                              Collections.singletonList(leafTree));
      return tagTree;
    }
    
    private Tree<String> getBestKnownParse(List<String> tags, List<String> sentence) {
      Tree<String> parse = knownParses.getCounter(tags).argMax().deepCopy();
      parse.setWords(sentence);
      return parse;
    }

    private List<String> getBaselineTagging(List<String> sentence) {
      List<String> tags = new ArrayList<String>();
      for (String word : sentence) {
        String tag = getBestTag(word);
        tags.add(tag);
      }
      return tags;
    }

    private String getBestTag(String word) {
      double bestScore = Double.NEGATIVE_INFINITY;
      String bestTag = null;
      for (String tag : lexicon.getAllTags()) {
        double score = lexicon.scoreTagging(word, tag);
        if (bestTag == null || score > bestScore) {
          bestScore = score;
          bestTag = tag;
        }
      }
      return bestTag;
    }

    private int tallySpans(Tree<String> tree, int start) {
      if (tree.isLeaf() || tree.isPreTerminal()) 
        return 1;
      int end = start;
      for (Tree<String> child : tree.getChildren()) {
        int childSpan = tallySpans(child, end);
        end += childSpan;
      }
      String category = tree.getLabel();
      if (! category.equals(ROOT_STR))
        spanToCategories.incrementCount(end - start, category, 1.0);
      return end - start;
    }

  }


  // TreeAnnotations ============================================================

  /**
   * Class which contains code for annotating and binarizing trees for
   * the parser's use, and debinarizing and unannotating them for
   * scoring.
   */
  public static class TreeAnnotations {

    public static List<Tree<String>> annotateAllTrees(List<Tree<String>> unAnnotatedTrees) {
      List<Tree<String>> toRet = new ArrayList<Tree<String>>();
      for (Tree<String> tree : unAnnotatedTrees) {
        toRet.add(annotateTree(tree));
      }
      return toRet;
    }
    
    public static Tree<String> annotateTree(Tree<String> unAnnotatedTree) {

      // Currently, the only annotation done is a lossless binarization

      // TODO: change the annotation from a lossless binarization to a
      // finite-order markov process (try at least 1st and 2nd order)

      // TODO : mark nodes with the label of their parent nodes, giving a second
      // order vertical markov process
      //verticalMarkovize(unAnnotatedTree);
      return binarizeTree(unAnnotatedTree);
    }
    
    public static void vertMarkovization(List<Tree<String>> trees) {
      for (Tree<String> tree : trees) {
        verticalMarkovize(tree);
      }
    }
    
    private static void verticalMarkovize(Tree<String> tree) {
      for (Tree<String> subTree : tree.getChildren()) {
        recursiveMarkovize(subTree, tree.getLabel());
      }
    }
    
    private static void recursiveMarkovize(Tree<String> tree, String parentLabel) {
      if (!tree.isLeaf()) {  // Don't annotate leaves...
        for (Tree<String> subTree : tree.getChildren()) {
          recursiveMarkovize(subTree, tree.getLabel());
        }
        //System.out.println("Tree label, before: " + tree.getLabel());
        tree.setLabel(tree.getLabel() + "-^" + parentLabel);
        //System.out.println("Tree label, after: " + tree.getLabel());
      }
    }
    
    private static Tree<String> binarizeTree(Tree<String> tree) {
      String label = tree.getLabel();
      if (tree.isLeaf())
        return new Tree<String>(label);
      if (tree.getChildren().size() == 1) {
        return new Tree<String>
          (label, 
           Collections.singletonList(binarizeTree(tree.getChildren().get(0))));
      }
      // otherwise, it's a binary-or-more local tree, 
      // so decompose it into a sequence of binary and unary trees.
      String intermediateLabel = "@"+label+"->";
      Tree<String> intermediateTree =
        binarizeTreeHelper(tree, 0, intermediateLabel);
      return new Tree<String>(label, intermediateTree.getChildren());
    }

    private static Tree<String> binarizeTreeHelper(Tree<String> tree,
                                                   int numChildrenGenerated, 
                                                   String intermediateLabel) {
      Tree<String> leftTree = tree.getChildren().get(numChildrenGenerated);
      List<Tree<String>> children = new ArrayList<Tree<String>>();
      children.add(binarizeTree(leftTree));
      if (numChildrenGenerated < tree.getChildren().size() - 1) {
        Tree<String> rightTree = 
          binarizeTreeHelper(tree, numChildrenGenerated + 1, 
                             intermediateLabel + "_" + leftTree.getLabel());
        children.add(rightTree);
      }
      return new Tree<String>(intermediateLabel, children);
    } 
 
    public static Tree<String> unAnnotateTree(Tree<String> annotatedTree) {

      // Remove intermediate nodes (labels beginning with "@"
      // Remove all material on node labels which follow their base symbol 
      // (cuts at the leftmost -, ^, or : character)
      // Examples: a node with label @NP->DT_JJ will be spliced out, 
     // and a node with label NP^S will be reduced to NP

      Tree<String> debinarizedTree =
        Trees.spliceNodes(annotatedTree, new Filter<String>() {
          public boolean accept(String s) {
            return s.startsWith("@");
          }
        });
      Tree<String> unAnnotatedTree = 
        (new Trees.FunctionNodeStripper()).transformTree(debinarizedTree);
      
      recDeMarcovize(unAnnotatedTree);
      return unAnnotatedTree;
    }
  }

  // Quick recursive check, since the Trees class seems to miss a few here and there...
  public static void recDeMarcovize(Tree<String> tree) {
    if (tree.isLeaf()) {
      return;
    }
    int index = tree.getLabel().indexOf("-^");
    if (index != -1) {
      tree.setLabel(tree.getLabel().substring(0, index));
    }
    for (Tree<String> subTree : tree.getChildren()) {
      recDeMarcovize(subTree);
    }
  }


  // Lexicon ====================================================================

  /**
   * Simple default implementation of a lexicon, which scores word,
   * tag pairs with a smoothed estimate of P(tag|word)/P(tag).
   */
  public static class Lexicon {

    CounterMap<String,String> wordToTagCounters = new CounterMap<String, String>();
    double totalTokens = 0.0;
    double totalWordTypes = 0.0;
    Counter<String> tagCounter = new Counter<String>();
    Counter<String> wordCounter = new Counter<String>();
    Counter<String> typeTagCounter = new Counter<String>();

    public Set<String> getAllTags() {
      return tagCounter.keySet();
    }

    public boolean isKnown(String word) {
      return wordCounter.keySet().contains(word);
    }

    /* Returns a smoothed estimate of P(word|tag) */
    public double scoreTagging(String word, String tag) {
      double p_tag = tagCounter.getCount(tag) / totalTokens;
      double c_word = wordCounter.getCount(word);
      double c_tag_and_word = wordToTagCounters.getCount(word, tag);
      if (c_word < 10) { // rare or unknown
        c_word += 1.0;
        c_tag_and_word += typeTagCounter.getCount(tag) / totalWordTypes;
      }
      double p_word = (1.0 + c_word) / (totalTokens + totalWordTypes);
      double p_tag_given_word = c_tag_and_word / c_word;
      return p_tag_given_word / p_tag * p_word;
    }

    /* Builds a lexicon from the observed tags in a list of training trees. */
    public Lexicon(List<Tree<String>> trainTrees) {
      for (Tree<String> trainTree : trainTrees) {
        List<String> words = trainTree.getYield();
        List<String> tags = trainTree.getPreTerminalYield();
        for (int position = 0; position < words.size(); position++) {
          String word = words.get(position);
          String tag = tags.get(position);
          tallyTagging(word, tag);
        }
      }
    }

    private void tallyTagging(String word, String tag) {
      if (! isKnown(word)) {
        totalWordTypes += 1.0;
        typeTagCounter.incrementCount(tag, 1.0);
      }
      totalTokens += 1.0;
      tagCounter.incrementCount(tag, 1.0);
      wordCounter.incrementCount(word, 1.0);
      wordToTagCounters.incrementCount(word, tag, 1.0);
    }
  }


  // Grammar ====================================================================

  /**
   * Simple implementation of a PCFG grammar, offering the ability to
   * look up rules by their child symbols.  Rule probability estimates
   * are just relative frequency estimates off of training trees.
   */
  public static class Grammar {

    Map<String, List<BinaryRule>> binaryRulesByLeftChild = 
      new HashMap<String, List<BinaryRule>>();
    Map<String, List<BinaryRule>> binaryRulesByRightChild = 
      new HashMap<String, List<BinaryRule>>();
    Map<String, List<UnaryRule>> rulesByChild = 
      new HashMap<String, List<UnaryRule>>();

    /* Rules in grammar are indexed by child for easy access when
     * doing bottom up parsing. */
    public List<BinaryRule> getBinaryRulesByLeftChild(String leftChild) {
      return CollectionUtils.getValueList(binaryRulesByLeftChild, leftChild);
    }

    public List<BinaryRule> getBinaryRulesByRightChild(String rightChild) {
      return CollectionUtils.getValueList(binaryRulesByRightChild, rightChild);
    }

    public List<UnaryRule> getrulesByChild(String child) {
      return CollectionUtils.getValueList(rulesByChild, child);
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      List<String> ruleStrings = new ArrayList<String>();
      for (String leftChild : binaryRulesByLeftChild.keySet()) {
        for (BinaryRule binaryRule : getBinaryRulesByLeftChild(leftChild)) {
          ruleStrings.add(binaryRule.toString());
        }
      }
      for (String child : rulesByChild.keySet()) {
        for (UnaryRule unaryRule : getrulesByChild(child)) {
          ruleStrings.add(unaryRule.toString());
        }
      }
      for (String ruleString : CollectionUtils.sort(ruleStrings)) {
        sb.append(ruleString);
        sb.append("\n");
      }
      return sb.toString();
    }

    private void addBinary(BinaryRule binaryRule) {
      CollectionUtils.addToValueList(binaryRulesByLeftChild, 
                                     binaryRule.getLeftChild(), binaryRule);
      CollectionUtils.addToValueList(binaryRulesByRightChild, 
                                     binaryRule.getRightChild(), binaryRule);
    }

    private void addUnary(UnaryRule unaryRule) {
      CollectionUtils.addToValueList(rulesByChild, 
                                     unaryRule.getChild(), unaryRule);
    }

    /* A builds PCFG using the observed counts of binary and unary
     * productions in the training trees to estimate the probabilities
     * for those rules.  */ 
    public Grammar(List<Tree<String>> trainTrees) {
      Counter<UnaryRule> unaryRuleCounter = new Counter<UnaryRule>();
      Counter<BinaryRule> binaryRuleCounter = new Counter<BinaryRule>();
      Counter<String> symbolCounter = new Counter<String>();
      for (Tree<String> trainTree : trainTrees) {
        tallyTree(trainTree, symbolCounter, unaryRuleCounter, binaryRuleCounter);
      }
      for (UnaryRule unaryRule : unaryRuleCounter.keySet()) {
        double unaryProbability = 
          unaryRuleCounter.getCount(unaryRule) / 
          symbolCounter.getCount(unaryRule.getParent());
        unaryRule.setScore(unaryProbability);
        addUnary(unaryRule);
      }
      for (BinaryRule binaryRule : binaryRuleCounter.keySet()) {
        double binaryProbability = 
          binaryRuleCounter.getCount(binaryRule) / 
          symbolCounter.getCount(binaryRule.getParent());
        binaryRule.setScore(binaryProbability);
        addBinary(binaryRule);
      }
    }

    private void tallyTree(Tree<String> tree, Counter<String> symbolCounter,
                           Counter<UnaryRule> unaryRuleCounter, 
                           Counter<BinaryRule> binaryRuleCounter) {
      if (tree.isLeaf()) return;
      if (tree.isPreTerminal()) return;
      if (tree.getChildren().size() == 1) {
        UnaryRule unaryRule = makeUnaryRule(tree);
        symbolCounter.incrementCount(tree.getLabel(), 1.0);
        unaryRuleCounter.incrementCount(unaryRule, 1.0);
      }
      if (tree.getChildren().size() == 2) {
        BinaryRule binaryRule = makeBinaryRule(tree);
        symbolCounter.incrementCount(tree.getLabel(), 1.0);
        binaryRuleCounter.incrementCount(binaryRule, 1.0);
      }
      if (tree.getChildren().size() < 1 || tree.getChildren().size() > 2) {
        throw new RuntimeException("Attempted to construct a Grammar with an illegal tree: "+tree);
      }
      for (Tree<String> child : tree.getChildren()) {
        tallyTree(child, symbolCounter, unaryRuleCounter,  binaryRuleCounter);
      }
    }

    private UnaryRule makeUnaryRule(Tree<String> tree) {
      return new UnaryRule(tree.getLabel(), tree.getChildren().get(0).getLabel());
    }

    private BinaryRule makeBinaryRule(Tree<String> tree) {
      return new BinaryRule(tree.getLabel(), tree.getChildren().get(0).getLabel(), 
                            tree.getChildren().get(1).getLabel());
    }
  }


  // Rule interface =============================================================
  
  /**
   * Forces rules through a common interface, which allows for polymorphism...
   */
  public static interface Rule {
    public String getParent();
    public double getScore();
    public void setScore(double score);
  }
  
  // BinaryRule =================================================================

  /* A binary grammar rule with score representing its probability. */
  public static class BinaryRule implements Rule {

    String parent;
    String leftChild;
    String rightChild;
    double score;

    public String getParent() {
      return parent;
    }

    public String getLeftChild() {
      return leftChild;
    }

    public String getRightChild() {
      return rightChild;
    }

    public double getScore() {
      return score;
    }

    public void setScore(double score) {
      this.score = score;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof BinaryRule)) return false;

      final BinaryRule binaryRule = (BinaryRule) o;

      if (leftChild != null ? !leftChild.equals(binaryRule.leftChild) : binaryRule.leftChild != null) 
        return false;
      if (parent != null ? !parent.equals(binaryRule.parent) : binaryRule.parent != null) 
        return false;
      if (rightChild != null ? !rightChild.equals(binaryRule.rightChild) : binaryRule.rightChild != null) 
        return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (parent != null ? parent.hashCode() : 0);
      result = 29 * result + (leftChild != null ? leftChild.hashCode() : 0);
      result = 29 * result + (rightChild != null ? rightChild.hashCode() : 0);
      return result;
    }

    public String toString() {
      return parent + " -> " + leftChild + " " + rightChild + " %% "+score;
    }

    public BinaryRule(String parent, String leftChild, String rightChild) {
      this.parent = parent;
      this.leftChild = leftChild;
      this.rightChild = rightChild;
    }
  }


  // UnaryRule ==================================================================

  /** A unary grammar rule with score representing its probability. */
  public static class UnaryRule implements Rule {

    String parent;
    String child;
    double score;

    public String getParent() {
      return parent;
    }

    public String getChild() {
      return child;
    }

    public double getScore() {
      return score;
    }

    public void setScore(double score) {
      this.score = score;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof UnaryRule)) return false;

      final UnaryRule unaryRule = (UnaryRule) o;

      if (child != null ? !child.equals(unaryRule.child) : unaryRule.child != null) return false;
      if (parent != null ? !parent.equals(unaryRule.parent) : unaryRule.parent != null) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (parent != null ? parent.hashCode() : 0);
      result = 29 * result + (child != null ? child.hashCode() : 0);
      return result;
    }

    public String toString() {
      return parent + " -> " + child + " %% "+score;
    }

    public UnaryRule(String parent, String child) {
      this.parent = parent;
      this.child = child;
    }
  }



  // PCFGParserTester ===========================================================

  // Longest sentence length that will be tested on.
  private static int MAX_LENGTH = 20;

 
  private static double testParser(Parser parser, List<Tree<String>> testTrees) {
    EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String> eval = 
      new EnglishPennTreebankParseEvaluator.LabeledConstituentEval<String>
      (Collections.singleton("ROOT"), 
       new HashSet<String>(Arrays.asList(new String[] {"''", "``", ".", ":", ","})));
    for (Tree<String> testTree : testTrees) {
      List<String> testSentence = testTree.getYield();

      if (testSentence.size() > MAX_LENGTH)
        continue;
      Tree<String> guessedTree = parser.getBestParse(testSentence);
      System.out.println("Guess:\n"+Trees.PennTreeRenderer.render(guessedTree));
      System.out.println("Gold:\n"+Trees.PennTreeRenderer.render(testTree));
      eval.evaluate(guessedTree, testTree);
    }
    System.out.println();
    return eval.display(true);
  }
  
  private static List<Tree<String>> readTrees(String basePath, int low,
			int high) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath,
				low, high);
		// normalize trees
		Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
		List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
		for (Tree<String> tree : trees) {
			Tree<String> normalizedTree = treeTransformer.transformTree(tree);
			// System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
			normalizedTreeList.add(normalizedTree);
		}
		return normalizedTreeList;
	}

	private static List<Tree<String>> readTrees(String basePath) {
		Collection<Tree<String>> trees = PennTreebankReader.readTrees(basePath);
		// normalize trees
		Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
	  List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
    for (Tree<String> tree : trees) {
      //      System.err.println(tree);
      Tree<String> normalizedTree = treeTransformer.transformTree(tree);
      // System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
      normalizedTreeList.add(normalizedTree);
    }
    return normalizedTreeList;
  }


  private static List<Tree<String>> readMASCTrees(String basePath, int low, int high) {
    System.out.println("MASC basepath: " + basePath);
    Collection<Tree<String>> trees = MASCTreebankReader.readTrees(basePath, low, high);
    // normalize trees
    Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
    List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
    for (Tree<String> tree : trees) {
      Tree<String> normalizedTree = treeTransformer.transformTree(tree);
      // System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
      normalizedTreeList.add(normalizedTree);
    }
    return normalizedTreeList;
  }


  private static List<Tree<String>> readMASCTrees(String basePath) {
    System.out.println("MASC basepath: " + basePath);
    Collection<Tree<String>> trees = MASCTreebankReader.readTrees(basePath);
    // normalize trees
    Trees.TreeTransformer<String> treeTransformer = new Trees.StandardTreeNormalizer();
    List<Tree<String>> normalizedTreeList = new ArrayList<Tree<String>>();
    for (Tree<String> tree : trees) {
      Tree<String> normalizedTree = treeTransformer.transformTree(tree);
      // System.out.println(Trees.PennTreeRenderer.render(normalizedTree));
      normalizedTreeList.add(normalizedTree);
    }
    return normalizedTreeList;
  }
  
  
  public static void main(String[] args) {

    // set up default options ..............................................
    Map<String, String> options = new HashMap<String, String>();
    options.put("--path",      "../data/parser/");
    options.put("--data",      "masc");
    options.put("--parser",    "nlpclass.assignments.PCFGParserTester$PCFGParser");
    options.put("--maxLength", "20");

    // let command-line options supersede defaults .........................
    options.putAll(CommandLineUtils.simpleCommandLineParser(args));
    System.out.println("PCFGParserTester options:");
    for (Map.Entry<String, String> entry: options.entrySet()) {
      System.out.printf("  %-12s: %s%n", entry.getKey(), entry.getValue());
    }
    System.out.println();

    MAX_LENGTH = Integer.parseInt(options.get("--maxLength"));

    Parser parser;
    try {
      Class parserClass = Class.forName(options.get("--parser"));
      parser = (Parser) parserClass.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    System.out.println("Using parser: " + parser);

    String basePath = options.get("--path");
    String preBasePath = basePath;
    String dataSet = options.get("--data");
    if (!basePath.endsWith("/"))
      basePath += "/";
    //basePath += dataSet;
    System.out.println("Data will be loaded from: " + basePath + "\n");

    List<Tree<String>> trainTrees = new ArrayList<Tree<String>>(),
    				   validationTrees = new ArrayList<Tree<String>>(),
    				   testTrees = new ArrayList<Tree<String>>();

    if (dataSet.equals("miniTest")) {
      // training data: first 3 of 4 datums
      basePath += "parser/"+dataSet;
      System.out.println("Loading training trees...");
      trainTrees = readTrees(basePath, 1, 3);
      System.out.println("done.");

      // test data: last of 4 datums
      System.out.println("Loading test trees...");
      testTrees = readTrees(basePath, 4, 4);
      System.out.println("done.");

    }
    if (dataSet.equals("masc")) {
      basePath += "parser/";
      // training data: MASC train
      System.out.println("Loading MASC training trees... from: "+basePath+"masc/train");
      trainTrees.addAll(readMASCTrees(basePath+"masc/train", 0, 38));
      System.out.println("done.");
      System.out.println("Train trees size: "+trainTrees.size());

      System.out.println("First train tree: "+Trees.PennTreeRenderer.render(trainTrees.get(0)));
      System.out.println("Last train tree: "+Trees.PennTreeRenderer.render(trainTrees.get(trainTrees.size()-1)));
      
      // test data: MASC devtest
      System.out.println("Loading MASC test trees...");
      testTrees.addAll(readMASCTrees(basePath+"masc/devtest", 0, 11));
      //testTrees.addAll(readMASCTrees(basePath+"masc/blindtest", 0, 8));
      System.out.println("Test trees size: "+testTrees.size());
      System.out.println("done.");
      
      System.out.println("First test tree: "+Trees.PennTreeRenderer.render(testTrees.get(0)));
      System.out.println("Last test tree: "+Trees.PennTreeRenderer.render(testTrees.get(testTrees.size()-1)));
    }
    if (!dataSet.equals("miniTest") && !dataSet.equals("masc")){
      throw new RuntimeException("Bad data set: " + dataSet + ": use miniTest or masc."); 
    }

    System.out.println("\nTraining parser...");
    parser.train(trainTrees);

    System.out.println("\nTesting parser...");
    testParser(parser, testTrees);
  }
}
