package spp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class Whitespace4j {
  private static byte LF = '\n';
  private byte[] _code;
  private int _pos;
  private StringBuilder _output;
  private InputStream _inputStream;
  private HashMap<Integer, Integer> _heap;
  private HashMap<String, Integer> _labels;
  private Stack _stack;
  private Stack _callstack;

  public Whitespace4j(byte[] code, StringBuilder output, InputStream inputStream) {
    _callstack = new Stack();
    _stack = new Stack();
    _labels = new HashMap<String, Integer>(32);
    _heap = new HashMap<Integer, Integer>(32);
    _code = code;
    _output = output;
    _inputStream = inputStream;
  }

  public void run() {
    int bye = 0;
    byte ch;
    do {
      ch = nextCh();
      if (ch == ' ') { // IMP Stack Manipulation
        impStackManipulation();
      } else if (ch == LF) { // IMP Flow Control
        bye = impFlowControl();
      } else { // '\t'
        ch = nextCh();
        if (ch == ' ') { // IMP Arithmentic
          impArithmentic();
        } else if (ch == LF) { // IMP I/O
          impIO();
        } else { // IMP Heap Access
          impHeapAccess();
        }
      }
    } while (bye == 0);
  }

  private int parseNumber() {
    int r = 0;
    byte ch = nextCh();
    int sign = (ch == ' ') ? 1 : -1;
    ch = nextCh();
    while (ch != LF) {
      r *= 2;
      if (ch == '\t') {
        r += 1;
      }
      ch = nextCh();
    }
    return r * sign;
  }

  private String parseLabel() {
    String r = "";
    byte ch = nextCh();
    while (ch != LF) {
      r += ch;
      ch = nextCh();
    }
    return r;
  }

  private void impStackManipulation() {
    byte ch = nextCh();
    if (ch == ' ') { // Push the number onto the stack
      int n = parseNumber();
      _stack.push(n);
    } else if (ch == LF) {
      ch = nextCh();
      if (ch == ' ') { // Duplicate the top item on the stack
        int n = _stack.top(0);
        _stack.push(n);
      } else if (ch == LF) { // Discard the top item on the stack
        _stack.pop();
      } else { // Swap the top two items on the stack
        int n1 = _stack.pop();
        int n2 = _stack.pop();
        _stack.push(n1);
        _stack.push(n2);
      }
    } else { // '\t'
      ch = nextCh();
      if (ch == ' ') { // Copy the nth item on the stack (given by the argument) onto the top of the stack
        int n1 = parseNumber();
        int n2 = _stack.top(n1);
        _stack.push(n2);
      } else if (ch == LF) { // Slide n items off the stack, keeping the top item
        int n = parseNumber();
        int v = _stack.pop();
        for (int i = 0; i < n; i++) {
          _stack.pop();
        }
        _stack.push(v);
      }
    }
  }

  private int impFlowControl() {
    int bye = 0;
    byte ch = nextCh();
    if (ch == ' ') {
      ch = nextCh();
      if (ch == ' ') { // Mark a location in the program
        String lbl = parseLabel();
        _labels.put(lbl, _pos);
      } else if (ch == '\t') { // Call a subroutine
        String lbl = parseLabel();
        _callstack.push(_pos);
        _pos = _labels.get(lbl);
      } else { // Jump unconditionally to a label
        String lbl = parseLabel();
        _pos = _labels.get(lbl);
      }
    } else if (ch == '\t') {
      ch = nextCh();
      if (ch == ' ') { // Jump to a label if the top of the stack is zero
        String lbl = parseLabel();
        int n = _stack.pop();
        if (n == 0) {
          _pos = _labels.get(lbl);
        }
      } else if (ch == LF) { // Jump to a label if the top of the stack is negative
        String lbl = parseLabel();
        int n = _stack.pop();
        if (n < 0) {
          _pos = _labels.get(lbl);
        }
      } else { // End a subroutine and transfer control back to the caller
        _pos = _callstack.pop();
      }
    } else { // LF
      // End the program
      bye = 1;
    }
    return bye;
  }

  private void impArithmentic() {
    byte ch = nextCh();
    int n2 = _stack.pop();
    int n1 = _stack.pop();
    if (ch == ' ') {
      ch = nextCh();
      if (ch == ' ') { // Operator +
        _stack.push(n1 + n2);
      } else if (ch == '\t') { // Operator -
        _stack.push(n1 - n2);
      } else { // Operator *
        _stack.push(n1 * n2);
      }
    } else { // '\t'
      ch = nextCh();
      if (ch == ' ') { // Operator /
        _stack.push(n1 / n2);
      } else { // Operator %
        _stack.push(n1 % n2);
      }
    }
  }

  private void impIO() {
    byte ch = nextCh();
    if (ch == ' ') {
      ch = nextCh();
      int n = _stack.pop();
      if (ch == ' ') { // put char
        putCh((char)n);
      } else { // put number
        String s = Integer.toString(n);
        for (int i = 0, l = s.length();i < l;i++) {
          putCh(s.charAt(i));
        }
      }
    } else { // '\t'
      ch = nextCh();
      if (ch == ' ') { // get char
        try {
          _stack.push(_inputStream.read());
        } catch (IOException e) {
          // ignore
        }
      } else { // get number
        try {
          int v = 0;
          int c = _inputStream.read();
          while (c >= '0' && c <= '9') {
            v *= 10;
            v += (c - '0');
            c = _inputStream.read();
          }
          _stack.push(v);
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  private void impHeapAccess() {
    byte ch = nextCh();
    if (ch == ' ') { // Store
      int n = _stack.pop();
      int adr = _stack.pop();
      _heap.put(Integer.valueOf(adr), Integer.valueOf(n));
    } else { // Retrieve
      int adr = _stack.pop();
      int n = _heap.get(adr);
      _stack.push(n);
    }
  }

  private byte nextCh() {
    byte ch;
    ch = _code[_pos++];
    while (ch != ' ' && ch != '\t' && ch != LF) {
      ch = _code[_pos++];
    }
    return ch;
  }

  private void putCh(char ch) {
    if (_output != null) {
      _output.append(ch);
    } else {
      System.out.print(ch);
    }
  }

  private class Stack {
    private ArrayList<Integer> _stack;
    
    public Stack() {
      _stack = new ArrayList<Integer>(32);
    }

    public void push(int v) {
      _stack.add(Integer.valueOf(v));
    }

    public int pop() {
      int top = _stack.size()-1;
      int r = get(top);
      _stack.remove(top);
      return r;
    }

    public int top(int n) {
      return (int)_stack.get(_stack.size() - 1 - n);
    }

    public int get(int idx) {
      return (int)_stack.get(idx);
    }
  }

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    String inputFname = "-";
    String mode = "run";

    if (args.length == 0) {
      System.out.println("Whitespace -f fname");
    }

    for (int p = 0, l = args.length; p < l; p += 2) {
      String n = args[p];
      String v = args[p + 1];
      if (n.equals("-f")) {
        inputFname = v;
      } else if (n.equals("-m")) {
        mode = v;
      }
    }

    // read input
    InputStream inputS;
    if (inputFname.equals("-")) {
      inputS = System.in;
    } else {
      inputS = new FileInputStream(inputFname);
    }
    byte[] code = new byte[32000];
    if (inputS.read(code) > 0) {
      if (mode.equalsIgnoreCase("run")) {
        Whitespace4j whitespace = new Whitespace4j(code, null, null);
        whitespace.run();
      }
    }
  }
}
