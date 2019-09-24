package revolut;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class ListNode {

  int val;
  ListNode next;

  ListNode(int x) {
    val = x;
    next = null;
  }
}

class TreeNode {

  int val;
  TreeNode left;
  TreeNode right;

  TreeNode(int x) {
    val = x;
  }
}

class Solution {

  public static final char VISITED = '\0';

  public boolean exist(char[][] board, String word) {
    if(board.length == 0 || word.isEmpty()) return false;

    char[] cs = word.toCharArray();

    for(int r = 0; r < board.length; r++) {
      for(int c = 0; c < board[0].length; c++) {
        if(board[r][c] == cs[0] && match(board, r, c, cs, 0)) return true;
      }
    }

    return false;
  }

  private boolean match(char[][] board, int r, int c, char[] word, int posw) {
    if(r < 0 || r >= board.length) return false;
    if(c < 0 || c >= board[0].length) return false;
    if(board[r][c] != word[posw]) return false;
    if(board[r][c] == word[posw] && posw+1 == word.length) return true;

    char curChar = board[r][c];
    board[r][c] = VISITED;
    boolean isMatch = match(board, r + 1, c, word, posw + 1)
        || match(board, r - 1, c, word, posw + 1)
        || match(board, r, c + 1, word, posw + 1)
        || match(board, r, c - 1, word, posw + 1);
    board[r][c] = curChar;

    return isMatch;
  }
}

public class Main {

  public static void main(String[] args) {
    System.out.printf("Solution: %s\n", new Solution().exist(new char[][]{{}}, ""));
  }
}