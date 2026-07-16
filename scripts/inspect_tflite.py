import sys
import tflite
from tflite.Model import Model
from tflite.BuiltinOperator import BuiltinOperator
from tflite.TensorType import TensorType

BUILTIN = {v: k for k, v in BuiltinOperator.__dict__.items() if isinstance(v, int)}
TTYPE = {v: k for k, v in TensorType.__dict__.items() if isinstance(v, int)}

def load(path):
    with open(path, "rb") as f:
        buf = bytearray(f.read())
    return Model.GetRootAsModel(buf, 0)

def opname(model, op):
    oc = model.OperatorCodes(op.OpcodeIndex())
    bi = oc.BuiltinCode()
    # newer schema: DeprecatedBuiltinCode for <127
    name = BUILTIN.get(bi, f"UNKNOWN({bi})")
    if bi == 32:  # CUSTOM
        cust = oc.CustomCode()
        name = f"CUSTOM:{cust.decode() if cust else '?'}"
    return name

def tinfo(sg, idx):
    t = sg.Tensors(idx)
    shape = [t.Shape(i) for i in range(t.ShapeLength())] if not t.ShapeIsNone() else []
    tt = TTYPE.get(t.Type(), t.Type())
    q = t.Quantization()
    qs = ""
    if q is not None and not q.ScaleIsNone():
        scl = q.ScaleLength(); zp = q.ZeroPointLength()
        sc0 = q.Scale(0) if scl else None
        zp0 = q.ZeroPoint(0) if zp else None
        qs = f" q[scale_n={scl},zp_n={zp},s0={sc0},zp0={zp0}]"
    nm = t.Name().decode() if t.Name() else ""
    return f"#{idx} {nm} {tt}{shape}{qs}"

def main(path, focus=None):
    model = load(path)
    print(f"==== {path}")
    print("version", model.Version())
    sg = model.Subgraphs(0)
    n = sg.OperatorsLength()
    print("operators:", n, "tensors:", sg.TensorsLength())
    ins = [sg.Inputs(i) for i in range(sg.InputsLength())]
    outs = [sg.Outputs(i) for i in range(sg.OutputsLength())]
    print("graph inputs:")
    for i in ins: print("   ", tinfo(sg, i))
    print("graph outputs:")
    for o in outs: print("   ", tinfo(sg, o))

    # op histogram
    from collections import Counter
    hist = Counter()
    for k in range(n):
        hist[opname(model, sg.Operators(k))] += 1
    print("op histogram:", dict(hist))

    if focus is not None:
        lo, hi = max(0, focus-4), min(n, focus+5)
        print(f"---- ops [{lo},{hi}) around node {focus}:")
        for k in range(lo, hi):
            op = sg.Operators(k)
            oin = [op.Inputs(i) for i in range(op.InputsLength())]
            oout = [op.Outputs(i) for i in range(op.OutputsLength())]
            mark = " <===" if k == focus else ""
            print(f"  node {k}: {opname(model, op)}{mark}")
            for ti in oin: print("      in ", tinfo(sg, ti))
            for to in oout: print("      out", tinfo(sg, to))

if __name__ == "__main__":
    path = sys.argv[1]
    focus = int(sys.argv[2]) if len(sys.argv) > 2 else None
    main(path, focus)
