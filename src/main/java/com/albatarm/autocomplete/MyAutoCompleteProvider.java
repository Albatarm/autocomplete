package com.albatarm.autocomplete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.antlr.v4.runtime.Lexer;

import com.albatarm.autocomplete.app.Caret;
import com.albatarm.autocomplete.app.DsnLang;
import com.dhatim.dsn.control.lang.v1.DsnLangLexer;

public class MyAutoCompleteProvider<T extends Lexer> implements AutoCompleteProvider {
	
	
	private static class PathItem {
		
	}
	
	private static class RulePathItem extends PathItem {
		
		private final String ruleName;

		public RulePathItem(String ruleName) {
			this.ruleName = ruleName;
		}
		
		public String getRuleName() {
			return ruleName;
		}
		
	}
	
	private static class TokenPathItem extends PathItem {
		
		private final int token;

		public TokenPathItem(int token) {
			this.token = token;
		}
		
		public int getToken() {
			return token;
		}
		
	}
	
	private static class NodePathItem extends PathItem {
		
		private final GrammarNode node;
		
		public NodePathItem(GrammarNode node) {
			this.node = node;
		}
		
		public GrammarNode getNode() {
			return node;
		}
		
	}
	
	private static class SequencePathItem extends PathItem {
		
		private final GrammarSequence sequence;
		private final int position;

		public SequencePathItem(GrammarSequence sequence, int position) {
			this.sequence = sequence;
			this.position = position;
		}
		
		public boolean isEnd() {
			return position >= sequence.getNodes().size();
		}
		
		public GrammarNode getCurrentNode() {
			return sequence.getNodes().get(position);
		}
		
		public SequencePathItem inc() {
			return new SequencePathItem(sequence, position + 1);
		}
		
	}
	
	private static class Path {
		
		private final List<PathItem> parts;
		
		private Path(List<PathItem> parts) {
			this.parts = new ArrayList<>(parts);
		}
		
		private Path(List<PathItem> parts, PathItem item) {
			this(parts);
			if (item != null) {
				this.parts.add(item);
			}
		}
		
		public static Path of(PathItem item) {
			return new Path(Collections.emptyList(), item);
		}
		
		public PathItem lastElement() {
			if (parts.isEmpty()) {
				throw new NoSuchElementException();
			}
			return parts.get(parts.size() - 1);
		}
		
		public Path concat(PathItem item) {
			return new Path(parts, item);
		}
		
		public Path removeLast() {
			return replaceLast(null);
		}
		
		public Path replaceLast(PathItem item) {
			if (parts.isEmpty()) {
				throw new NoSuchElementException();
			}
			return new Path(parts.subList(0, parts.size() - 1), item);
		}
		
	}
	
	private final AutoCompleter rulesHolder;
	private final List<String> tokenNames;
	private final Scanner<T> scanner;
	private final String rootRule;
	private final Caret caret;
	
	private final HashSet<String> candidates = new HashSet<>();
	private final HashSet<String> collectedRules = new HashSet<>();
	
	private final HashSet<Path> list = new HashSet<>();
	
	public MyAutoCompleteProvider(AutoCompleter rulesHolder, List<String> tokenNames, Scanner<T> scanner, String rootRule, Caret caret) {
		this.rulesHolder = rulesHolder;
		this.tokenNames = tokenNames;
		this.scanner = scanner;
		this.rootRule = rootRule;
		this.caret = caret;
    }

	@Override
	public boolean collectCandidates() {
		collectFromRule(rootRule);
		return false;
	}

	@Override
	public Set<String> getCandidates() {
		return Collections.unmodifiableSet(candidates);
	}
	
	private void prePopulate() {
		list.add(Path.of(new RulePathItem(rootRule)));
	}
	
	private boolean expand() {
		boolean result = false;
		HashSet<Path> paths = new HashSet<>();
		boolean changed = false;
		for (Path path : list) {
			changed |= expandPath(paths, path);
		}
		return changed;
	}
	
	private boolean expandPath(Set<Path> paths, Path path) {
		boolean changed;
		PathItem item = path.lastElement();
		if (item instanceof RulePathItem) {
			RuleAlternatives alt = rulesHolder.getRuleAlternatives(((RulePathItem) item).getRuleName());
			if (alt.isOptimized()) {
				alt.getTokens().stream().forEach(token -> {
					paths.add(path.concat(new TokenPathItem(token)));
				});
				changed = !alt.getTokens().isEmpty();
			} else {
				alt.getSequences().stream().forEach(sequence -> {
					paths.add(path.concat(new SequencePathItem(sequence, 0)));
				});
				changed = !alt.getSequences().isEmpty();
			}
		} else if (item instanceof SequencePathItem) {
			SequencePathItem seq = (SequencePathItem) item;
			if (seq.isEnd()) {
				paths.add(path.removeLast());
				changed = true;
			} else {
				GrammarNode node = seq.getCurrentNode();
				PathItem newItem = node.isTerminal() ? new TokenPathItem(node.getTokenRef()) : new RulePathItem(node.getRuleRef());
				if (node.isRequired()) {
					paths.add(path.concat(newItem));
					changed = true;
				} else {
					paths.add(path.concat(newItem));
					paths.add(path.replaceLast(seq.inc()));
					changed = true;
				}
			}
		} else if (item instanceof TokenPathItem) {
			paths.add(path);
			changed = false;
		} else {
			throw new IllegalStateException();
		}
		return changed;
	}
	
	private void matchRule(String rule, int tokenType) {
		
	}
	
	private boolean collectFromRule(String rule) {
		if (collectedRules.contains(rule)) {
			return true;
		}
		collectedRules.add(rule);
		
		RuleAlternatives alt = rulesHolder.getRuleAlternatives(rule);
		if (alt.isOptimized()) {
			alt.getTokens().forEach(this::addCandidate);
			return !alt.getTokens().isEmpty();
		} else {
			boolean collected = false;
			for (GrammarSequence seq : alt.getSequences()) {
				collected |= collectFromSequence(seq);
			}
			return collected;
		}
	}
	
	private boolean collectFromSequence(GrammarSequence sequence) {
		Iterator<GrammarNode> it = sequence.getNodes().iterator();
		while (it.hasNext()) {
			GrammarNode node = it.next();
			if (node.isRequired()) {
				if (node.isTerminal()) {
					addCandidate(node.getTokenRef());
					return true;
				} else {
					if (collectFromRule(node.getRuleRef())) {
						return true;
					}
				}
			} 
		}
		return false;
	}
	
	private void addCandidate(int tokenId) {
		candidates.add(tokenNames.get(tokenId));
	}
	
	private boolean isTokenEndAfterCaret() {
        if (scanner.isType(T.EOF)) {
            return true;
        }
        assert (scanner.getTokenLine() > 0);
        if (scanner.getTokenLine() > caret.getLine()) {
            return true;
        }
        if (scanner.getTokenLine() < caret.getLine()) {
            return false;
        }

        // This determination is a bit tricky as it depends on the type of the token.
        // For letters (like when typing a keyword) all positions directly attached to a letter must be
        // considered within the token (as we could extend it).
        // For example each vertical bar is a position within the token: |F|R|O|M|
        // Not so with tokens that can separate other tokens without the need of a whitespace (comma etc.).

        boolean result;
        if (scanner.isSeparator()) {
            result = scanner.getTokenEnd() > caret.getOffset();
        } else {
            result = scanner.getTokenEnd() >= caret.getOffset();
        }
        return result;
    }
	
	public static void main(String[] args) {
		DsnLang lang = new DsnLang();
		MyAutoCompleteProvider<DsnLangLexer> provider = lang.create("", Caret.atStart());
		provider.collectCandidates();
		lang.getAutoCompleter().print();
		System.out.println(provider.getCandidates());
	}
	
}
