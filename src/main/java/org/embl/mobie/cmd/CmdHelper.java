package org.embl.mobie.cmd;

public class CmdHelper
{
	// from https://stackoverflow.com/questions/28734455/java-converting-file-pattern-to-regular-expression-pattern
	public static String wildcardToRegex(String wildcard){
		StringBuffer s = new StringBuffer(wildcard.length());
		s.append('^');
		for (int i = 0, is = wildcard.length(); i < is; i++) {
			char c = wildcard.charAt(i);
			switch(c) {
				case '*':
					s.append(".*");
					break;
				case '?':
					s.append(".");
					break;
				case '^': // escape character in cmd.exe
					s.append("\\");
					break;
				// escape special regexp-characters
				case '(': case ')': case '[': case ']': case '$':
				case '.': case '{': case '}': case '|':
				case '\\':
					s.append("\\");
					s.append(c);
					break;
				default:
					s.append(c);
					break;
			}
		}
		s.append('$');
		return(s.toString());
	}
}
