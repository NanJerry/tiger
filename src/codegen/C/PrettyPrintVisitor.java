package codegen.C;

import codegen.C.Ast.Class.ClassSingle;
import codegen.C.Ast.Dec;
import codegen.C.Ast.Dec.DecSingle;
import codegen.C.Ast.Exp;
import codegen.C.Ast.Exp.Add;
import codegen.C.Ast.Exp.And;
import codegen.C.Ast.Exp.ArraySelect;
import codegen.C.Ast.Exp.Call;
import codegen.C.Ast.Exp.Id;
import codegen.C.Ast.Exp.Length;
import codegen.C.Ast.Exp.Lt;
import codegen.C.Ast.Exp.NewIntArray;
import codegen.C.Ast.Exp.NewObject;
import codegen.C.Ast.Exp.Not;
import codegen.C.Ast.Exp.Num;
import codegen.C.Ast.Exp.Sub;
import codegen.C.Ast.Exp.This;
import codegen.C.Ast.Exp.Times;
import codegen.C.Ast.MainMethod.MainMethodSingle;
import codegen.C.Ast.Method;
import codegen.C.Ast.Method.MethodSingle;
import codegen.C.Ast.Program.ProgramSingle;
import codegen.C.Ast.Stm;
import codegen.C.Ast.Stm.Assign;
import codegen.C.Ast.Stm.AssignArray;
import codegen.C.Ast.Stm.Block;
import codegen.C.Ast.Stm.If;
import codegen.C.Ast.Stm.Print;
import codegen.C.Ast.Stm.T;
import codegen.C.Ast.Stm.While;
import codegen.C.Ast.Type.ClassType;
import codegen.C.Ast.Type.Int;
import codegen.C.Ast.Type.IntArray;
import codegen.C.Ast.Vtable;
import codegen.C.Ast.Vtable.VtableSingle;
import control.Control;

import java.util.HashMap;
import java.util.LinkedList;

public class PrettyPrintVisitor implements Visitor {
    private int indentLevel;
    private java.io.BufferedWriter writer;
    private HashMap<String, String> classGcMap = new HashMap<>();
    private LinkedList<String> refDec = new LinkedList<String>();

    public PrettyPrintVisitor() {
        this.indentLevel = 2;
    }

    private void indent() {
        this.indentLevel += 2;
    }

    private void unIndent() {
        this.indentLevel -= 2;
    }

    private void printSpaces() {
        int i = this.indentLevel;
        while (i-- != 0)
            this.say(" ");
    }

    private void sayln(String s) {
        say(s);
        try {
            this.writer.write("\n");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void say(String s) {
        try {
            this.writer.write(s);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    // /////////////////////////////////////////////////////
    // expressions
    @Override
    public void visit(Add e) {
        e.left.accept(this);
        this.say(" + ");
        e.right.accept(this);
    }

    @Override
    public void visit(And e) {
        e.left.accept(this);
        this.say(" && ");
        e.right.accept(this);
    }

    @Override
    public void visit(ArraySelect e) {
        e.array.accept(this);
        this.say("[");
        e.index.accept(this);
        this.say("]");
    }

    @Override
    public void visit(Call e) {
        if (refDec.contains(e.assign)) {
            this.say("(frame." + e.assign + "=");
            e.exp.accept(this);
            this.say(", ");
            this.say("frame." + e.assign + "->vptr->" + e.id + "(frame." + e.assign);
        } else {
            this.say("(" + e.assign + "=");
            e.exp.accept(this);
            this.say(", ");
            this.say(e.assign + "->vptr->" + e.id + "(" + e.assign);
        }
        int size = e.args.size();
        if (size == 0) {
            this.say("))");
            return;
        }
        for (Exp.T x : e.args) {
            this.say(", ");
            x.accept(this);
        }
        this.say("))");

        return;
    }

    @Override
    public void visit(Id e) {
        if (e.isField) {
            this.say("this->" + e.id);
        } else {
            if (refDec.contains(e.id))
                this.say("frame." + e.id);
            else this.say(e.id);
        }
    }

    @Override
    public void visit(Length e) {
        e.array.accept(this);
        this.say("[-1]");
    }

    @Override
    public void visit(Lt e) {
        e.left.accept(this);
        this.say(" < ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(NewIntArray e) {
        say("(int *)Tiger_new_array(");
        e.exp.accept(this);
        say(")");
    }

    @Override
    public void visit(NewObject e) {
        this.say("((struct " + e.id + "*)(Tiger_new (&" + e.id
                + "_vtable_, sizeof(struct " + e.id + "))))");
        return;
    }

    @Override
    public void visit(Not e) {
        this.say("!(");
        e.exp.accept(this);
        this.say(")");
    }

    @Override
    public void visit(Num e) {
        this.say(Integer.toString(e.num));
        return;
    }

    @Override
    public void visit(Sub e) {
        e.left.accept(this);
        this.say(" - ");
        e.right.accept(this);
        return;
    }

    @Override
    public void visit(This e) {
        this.say("this");
    }

    @Override
    public void visit(Times e) {
        e.left.accept(this);
        this.say(" * ");
        e.right.accept(this);
        return;
    }

    // statements
    @Override
    public void visit(Assign s) {
        this.printSpaces();
        s.id.accept(this);
        this.say(" = ");
        s.exp.accept(this);
        this.sayln(";");
        return;
    }

    @Override
    public void visit(AssignArray s) {
        printSpaces();
        s.id.accept(this);
        this.say("[");
        s.index.accept(this);
        this.say("] = ");
        s.exp.accept(this);
        this.sayln(";");
    }

    @Override
    public void visit(Block s) {
        for (T tmp : s.stms) {
            tmp.accept(this);
        }
    }

    @Override
    public void visit(If s) {
        this.printSpaces();
        this.say("if (");
        s.condition.accept(this);
        this.sayln("){");
        this.indent();
        s.thenn.accept(this);
        this.unIndent();
        this.printSpaces();
        this.sayln("}");
        this.printSpaces();
        this.sayln("else{");
        this.indent();
        s.elsee.accept(this);
        // this.sayln("");
        this.unIndent();
        this.printSpaces();
        this.sayln("}");
        return;
    }

    @Override
    public void visit(Print s) {
        this.printSpaces();
        this.say("System_out_println (");
        s.exp.accept(this);
        this.sayln(");");
        return;
    }

    @Override
    public void visit(While s) {
        this.printSpaces();
        this.say("while (");
        s.condition.accept(this);
        this.sayln("){");
        this.indent();
        s.body.accept(this);
        this.unIndent();
        this.printSpaces();
        this.sayln("}");
    }

    // type
    @Override
    public void visit(ClassType t) {
        this.say("struct " + t.id + " *");
    }

    @Override
    public void visit(Int t) {
        this.say("int");
    }

    @Override
    public void visit(IntArray t) {
        this.say("int*");
    }

    // dec
    @Override
    public void visit(DecSingle d) {
        d.type.accept(this);
        this.say(" ");
        this.say(d.id);
    }

    // method
    @Override
    public void visit(MethodSingle m) {
        StringBuffer sbForm = new StringBuffer();
        StringBuffer sbLoc = new StringBuffer();
        refDec.clear();
        m.retType.accept(this);
        this.say(" " + m.classId + "_" + m.id + "(");
        int size = m.formals.size();
        for (Dec.T d : m.formals) {
            DecSingle dec = (DecSingle) d;
            size--;
            dec.type.accept(this);
            if (dec.type.toString().equals("@int"))
                sbForm.append('0');
            else sbForm.append('1');
            this.say(" " + dec.id);
            if (size > 0)
                this.say(", ");
        }
        this.sayln(")");
        this.sayln("{");
        this.sayln("  struct " + m.id + "_gc_frame frame;");
        this.sayln("  frame.prev = prev;");
        this.sayln("  prev = &frame;");
        this.sayln("  frame.args_base_add = &this;");
        for (Dec.T d : m.locals) {
            DecSingle dec = (DecSingle) d;
            this.say("  ");
            if (dec.type.toString().equals("@int")) {
                sbLoc.append('0');
                dec.type.accept(this);
                this.say(" " + dec.id + ";\n");
            } else {
                sbLoc.append('1');
                this.sayln("frame." + dec.id + " = 0;");
                refDec.add(dec.id);
            }
        }
        this.sayln("");
        this.sayln("  frame.args_gc_map = \"" + sbForm.toString() + "\";");
        this.sayln("  frame.locals_gc_map = \"" + sbLoc.toString() + "\";");
        this.sayln("");
        for (Stm.T s : m.stms)
            s.accept(this);
        this.sayln("  prev = frame.prev;");
        this.say("  return ");
        m.retExp.accept(this);
        this.sayln(";");
        this.sayln("}");
        return;
    }

    @Override
    public void visit(MainMethodSingle m) {
        this.sayln("int Tiger_main ()");
        this.sayln("{");
        for (Dec.T dec : m.locals) {
            this.say("  ");
            DecSingle d = (DecSingle) dec;
            d.type.accept(this);
            this.say(" ");
            this.sayln(d.id + ";");
        }
        m.stm.accept(this);
        this.sayln("}\n");
        return;
    }

    // vtables
    @Override
    public void visit(VtableSingle v) {
        this.sayln("struct " + v.id + "_vtable");
        this.sayln("{");
        this.sayln("  char *" + v.id + "_gc_map;");
        for (codegen.C.Ftuple t : v.ms) {
            this.say("  ");
            t.ret.accept(this);
            this.sayln(" (*" + t.id + ")();");
        }
        this.sayln("};\n");
        return;
    }

    private void outputVtable(VtableSingle v) {
        this.sayln("struct " + v.id + "_vtable " + v.id + "_vtable_ = ");
        this.sayln("{");
        this.sayln("  \"" + classGcMap.get(v.id) + "\",");
        if (!v.ms.isEmpty()) {
            codegen.C.Ftuple fTmp = v.ms.get(0);
            this.say("  ");
            this.say(fTmp.classs + "_" + fTmp.id);
            v.ms.remove(0);
        }
        for (codegen.C.Ftuple t : v.ms) {
            this.sayln(",");
            this.say("  ");
            this.say(t.classs + "_" + t.id);
        }
        this.sayln("");
        this.sayln("};\n");
        return;
    }

    // class
    @Override
    public void visit(ClassSingle c) {
        StringBuffer sb = new StringBuffer();
        this.sayln("struct " + c.id);
        this.sayln("{");
        this.sayln("  struct " + c.id + "_vtable *vptr;");
        this.sayln("  int isArray;");
        this.sayln("  int length;");
        this.sayln("  void* forwarding;");
        for (codegen.C.Tuple t : c.decs) {
            this.say("  ");
            t.type.accept(this);
            this.say(" ");
            this.sayln(t.id + ";");
            if (t.type.toString().equals("@int"))
                sb.append('0');
            else sb.append('1');
        }
        this.sayln("};");
        classGcMap.put(c.id, sb.toString());
        return;
    }

    // program
    @Override
    public void visit(ProgramSingle p) {
        // we'd like to output to a file, rather than the "stdout".
        try {
            String outputName = null;
            if (Control.ConCodeGen.outputName != null)
                outputName = Control.ConCodeGen.outputName;
            else if (Control.ConCodeGen.fileName != null)
                outputName = Control.ConCodeGen.fileName + ".c";
            else
                outputName = "a.c";

            this.writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(
                            new java.io.FileOutputStream(outputName)));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        this.sayln("// This is automatically generated by the Tiger compiler.");
        this.sayln("// Do NOT modify!\n");

        this.sayln("extern void* Tiger_new(void*, int);");
        this.sayln("extern void* Tiger_new_array(int);");
        this.sayln("extern int System_out_println(int);");
        this.sayln("\n //glabal gc pointer");
        this.sayln("extern void *prev;\n");
        this.sayln("// structures");
        for (codegen.C.Ast.Class.T c : p.classes) {
            c.accept(this);
        }

        this.sayln("// GC_Frame");
        for (Method.T m : p.methods) {
            MethodSingle ms = (MethodSingle) m;
            sayln("struct  " + ms.id + "_gc_frame{");
            sayln("  void *prev;");
            sayln("  char *args_gc_map;");
            sayln("  int *args_base_add;");
            sayln("  char *locals_gc_map;");
            for (Dec.T dec : ms.locals) {
                DecSingle decs = (DecSingle) dec;
                say("  ");
                decs.type.accept(this);
                sayln(" " + decs.id + ";");
            }
            sayln("};");
        }

        this.sayln("// vtables structures");
        for (Vtable.T v : p.vtables) {
            v.accept(this);
        }
        this.sayln("");

//		this.sayln("// methods");
//		for (Method.T m : p.methods) {
//			m.accept(this);
//		}
//		this.sayln("");

        this.sayln("// methods declare");
        for (Method.T m : p.methods) {
            Method.MethodSingle ms = (MethodSingle) m;
            ms.retType.accept(this);
            this.say(" " + ms.classId + "_" + ms.id + "(");
            int size = ms.formals.size();
            for (Dec.T d : ms.formals) {
                DecSingle dec = (DecSingle) d;
                size--;
                dec.type.accept(this);
                this.say(" " + dec.id);
                if (size > 0)
                    this.say(", ");
            }
            this.sayln(");");
        }
        this.sayln("");

        this.sayln("// vtables");
        for (Vtable.T v : p.vtables) {
            outputVtable((VtableSingle) v);
        }
        this.sayln("");

        this.sayln("// main method");
        p.mainMethod.accept(this);
        this.sayln("");

        this.sayln("// methods");
        for (Method.T m : p.methods) {
            m.accept(this);
        }
        this.sayln("");
        this.say("\n\n");

        try {
            this.writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

}
