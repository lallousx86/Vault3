/*
  Vault 3
  (C) Copyright 2009, Eric Bergman-Terrell
  
  This file is part of Vault 3.

    Vault 3 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Vault 3 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Vault 3.  If not, see <http://www.gnu.org/licenses/>.
*/

package mainPackage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Search {
	public enum SearchMode { titles, titlesAndText }
	
	public static class SearchResults {
		private List<OutlineItem> results;
		
		public List<OutlineItem> getResults() {
			return results;
		}
		
		private Pattern[] patterns;
		
		public Pattern[] getPatterns() {
			return patterns;
		}
		
		public SearchResults(List<OutlineItem> results, Pattern[] patterns) {
			this.results = results;
			this.patterns = patterns;
		}
	}

	public static String[] getSearchTokens(String searchText) {
		// Ensure that double quotes are paired.
		int doubleQuotes = 0;
		
		for (int i = 0; i < searchText.length(); i++) {
			if (searchText.charAt(i) == '"') {
				doubleQuotes++;
			}
		}
		
		// If there are an odd number of double quotes, add one to the end.
		if ((doubleQuotes % 2) == 1) {
			searchText += '"';
		}
		
		StringBuilder unquotedText = new StringBuilder();

		StringBuilder quotedText = new StringBuilder();
		ArrayList<String> quotedTextList = new ArrayList<>();
		
		boolean insideQuotes = false;
		
		for (int i = 0; i < searchText.length(); i++) {
			char ch = searchText.charAt(i);

			if (ch == '"') {
				if (insideQuotes) {
					quotedTextList.add(quotedText.toString());
					quotedText.setLength(0);
					unquotedText.append(' ');
				}
				
				insideQuotes = !insideQuotes;
			}
			else {
				if (!insideQuotes) {
					unquotedText.append(ch);
				}
				else {
					quotedText.append(ch);
				}
			}
		}

		// Create a list containing all tokens, quoted and unquoted.
		List<String> tokenList = new ArrayList<>();
		tokenList.addAll(quotedTextList);
		
		String[] unquotedTokens = unquotedText.toString().split(" ");

		Collections.addAll(tokenList, unquotedTokens);

		List<String> finalTokenList = new ArrayList<>();
		
		for (String token : tokenList) {
			token = token.trim();
			
			// Remove empty and blank tokens.
			if (token.length() > 0) {
				finalTokenList.add(token);
			}
		}

		return finalTokenList.toArray(new String[finalTokenList.size()]);
	}

	/**
	 * Return an array of Pattern objects based on the search text and search options
	 * @param searchText search text
	 * @param matchCase true if case-sensitive searching has been requested
	 * @param fullWord true if entire words should be matched
	 * @return array of Pattern objects
	 */
	public static Pattern[] getSearchPatterns(String searchText, boolean matchCase, boolean fullWord) {
		String[] searchTokens = getSearchTokens(searchText);
		
		Globals.getLogger().info("Search Tokens:");
		
		for (String searchToken : searchTokens) {
			Globals.getLogger().info(searchToken);
		}
	
		List<Pattern> patterns = new ArrayList<>(searchTokens.length);
		
		Globals.getLogger().info("Search regular expressions:");
		
		String wordBoundary = fullWord ? "\\b" : "";
		
		for (String searchToken : searchTokens) {
			Pattern pattern = matchCase ? Pattern.compile(wordBoundary + Pattern.quote(searchToken) + wordBoundary) : Pattern.compile(wordBoundary + Pattern.quote(searchToken) + wordBoundary, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
			patterns.add(pattern);
			
			Globals.getLogger().info(searchToken);
		}
		
		return patterns.toArray(new Pattern[patterns.size()]);
	}
	
	public static SearchResults DoSearch(String searchText, boolean searchSelected, boolean matchCase, boolean fullWord, boolean matchAll, SearchMode searchMode) {
		Pattern[] patterns = getSearchPatterns(searchText, matchCase, fullWord);

		List<OutlineItem> itemsToSearch = new ArrayList<>();
		
		if (searchSelected) {
			itemsToSearch.addAll(Globals.getVaultTreeViewer().getSelectedItems());
		}
		else {
			itemsToSearch.add(Globals.getVaultDocument().getContent());
		}
		
		List<OutlineItem> allSearchHits = new ArrayList<>();
		
		for (OutlineItem outlineItem : itemsToSearch) {
			List<OutlineItem> searchHits = outlineItem.search(patterns, matchAll, searchMode);
			allSearchHits.addAll(searchHits);
		}

		return new Search.SearchResults(allSearchHits, patterns);
	}
}
