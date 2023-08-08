package midend;

import ir.*;
import ir.instruction.*;
import ir.type.Int32Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class GVN extends Pass{

    private HashMap<String, Instr> GVNMap = new HashMap<>();
    private HashMap<String, Integer> GVNCount = new HashMap<>();

    public GVN(HashMap<String, Function> functions, ArrayList<GlobalValue> globals) {
        super(functions, globals);
    }

    @Override
    public void run()
    {
        for(Function function : this.functions.values())
        {
            if(function.isExternal()) continue;
            runForFunc(function);
        }
    }

    private void runForFunc(Function function){
        BasicBlock entryBlock = function.getBasicBlocks().get(0);
        RPOSearch(entryBlock);
    }

    private void RPOSearch(BasicBlock bb){
        for(Instr instr:bb.getInstrs()){
            if(instr instanceof Binary){
                Binary binary = (Binary) instr;
                if(binary.isLeftConst() && binary.isRightConst()){
                    Value freshVal = calcConstant(binary);
                    binary.repalceUseofMeto(freshVal);
                    binary.remove();
                }
            }
            else if(instr instanceof Fptosi){
                Fptosi fptosi = (Fptosi) instr;
                if(fptosi.getValue() instanceof Variable.ConstFloat){
                    float val = (float)((Variable.ConstFloat) fptosi.getValue()).getFloatVal();
                    fptosi.repalceUseofMeto(new Variable.ConstInt((int)val));
                    fptosi.remove();
                }
            }
            else if(instr instanceof Sitofp){
                Sitofp sitofp = (Sitofp) instr;
                if(sitofp.getValue() instanceof Variable.ConstInt){
                    int val = (int)((Variable.ConstInt) sitofp.getValue()).getIntVal();
                    sitofp.repalceUseofMeto(new Variable.ConstFloat((float)val));
                    sitofp.remove();
                }
            }
        }
        HashSet<Instr> instrs = new HashSet<>();
        for(Instr instr:bb.getInstrs()){
            if(canGVN(instr)){
                String key = getHashofInstr(instr);
                if(GVNMap.containsKey(key)){
                    instrs.add(instr);
                }
                else{
                    GVNMap.put(key, instr);
                }
            }
        }
        for(BasicBlock next:bb.getIDoms()){
            RPOSearch(next);
        }
        for(Instr instr:instrs){
            instr.remove();
        }
    }

    private boolean canGVN(Instr instr){
        if(instr instanceof Call){
            Call call = (Call) instr;
            return (!call.getFunction().isExternal()) && call.getFunction().canGVN();
        }
        else{
            return instr instanceof Binary || instr instanceof Icmp || instr instanceof GetElementPtr || instr instanceof Fptosi || instr instanceof Sitofp;
        }
    }

    private Value calcConstant(Binary binary) {
        if (binary.isLeftConst() && binary.isRightConst()) {
            if(binary.getType() instanceof Int32Type){
                int left = ((Variable.ConstInt) binary.getLeft()).getIntVal();
                int right = ((Variable.ConstInt) binary.getRight()).getIntVal();
                switch (binary.getOp()) {
                    case Add:
                        return new Variable.ConstInt(left + right);
                    case Sub:
                        return new Variable.ConstInt(left - right);
                    case Mul:
                        return new Variable.ConstInt(left * right);
                    case Div:
                        return new Variable.ConstInt(left / right);
                    case Mod:
                        return new Variable.ConstInt(left % right);
                    case And:
                        return new Variable.ConstInt(left & right);
                    case Or:
                        return new Variable.ConstInt(left | right);
                    default:
                        return null;
                }
            }
            else{
                float left = ((Variable.ConstFloat) binary.getLeft()).getFloatVal();
                float right = ((Variable.ConstFloat) binary.getRight()).getFloatVal();
                switch (binary.getOp()) {
                    case Add:
                        return new Variable.ConstFloat(left + right);
                    case Sub:
                        return new Variable.ConstFloat(left - right);
                    case Mul:
                        return new Variable.ConstFloat(left * right);
                    case Div:
                        return new Variable.ConstFloat(left / right);
                    case Mod:
                        return new Variable.ConstFloat(left % right);
                    default:
                        return null;
                }
            }
        }
        else{
            assert false;
            return null;
        }
    }

    private String getHashofInstr(Instr instr){
        if(instr instanceof Binary){
            Binary binary = (Binary) instr;
            if(binary.getType() instanceof  Int32Type)
            return binary.getOp().toString() + binary.getLeft().toString() + binary.getRight().toString();
            else{
                return binary.getOp().getfName() + binary.getLeft().toString() + binary.getRight().toString();
            }
        }
        else if(instr instanceof Icmp){
            Icmp icmp = (Icmp) instr;
            return icmp.getOp().toString() + icmp.getLhs().toString() + icmp.getRhs().toString();
        }
        else if(instr instanceof GetElementPtr){
            GetElementPtr getElementPtr = (GetElementPtr) instr;
            String ret = getElementPtr.getPointer().toString();
            for(Value value:getElementPtr.getIdxList()){
                ret += value.toString();
            }
            return ret;
        }
        else if(instr instanceof Call){
            Call call = (Call) instr;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(call.getFunction().getName());
            for(int i=0;i<call.getParams().size();i++)
            {
                stringBuilder.append(call.getParams().get(i).toString());
                if(i!=call.getParams().size()-1)
                    stringBuilder.append(",");
            }
            return stringBuilder.toString();
        }
        else if(instr instanceof Fptosi){
            Fptosi fptosi = (Fptosi) instr;
            return "Fptosi"+fptosi.getValue().toString();
        }
        else if(instr instanceof Sitofp){
            Sitofp sitofp = (Sitofp) instr;
            return "Sitofp"+sitofp.getValue().toString();
        }
        else{
            assert false;
            return null;
        }
    }
}