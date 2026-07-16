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
    if bi == 32:
        c = oc.CustomCode()
        return f"CUSTOM:{c.decode() if c else '?'}"
    return BUILTIN.get(bi, f"UNKNOWN({bi})")

def tinfo(model, sg, idx):
    t = sg.Tensors(idx)
    shape = [t.Shape(i) for i in range(t.ShapeLength())] if not t.ShapeIsNone() else []
    tt = TTYPE.get(t.Type(), t.Type())
    q = t.Quantization()
    qs = ""
    if q is not None and not q.ScaleIsNone():
        scl = q.ScaleLength(); zp = q.ZeroPointLength()
        qd = q.QuantizedDimension()
        s0 = q.Scale(0) if scl else None
        z0 = q.ZeroPoint(0) if zp else None
        qs = f" q[n_scale={scl},zp={zp},dim={qd},s0={s0},zp0={z0}]"
    nm = t.Name().decode() if t.Name() else ""
    bi = t.Buffer()
    buf = model.Buffers(bi) if bi is not None else None
    isconst = buf is not None and not buf.DataIsNone() and buf.DataLength() > 0
    return f"#{idx} {nm} {tt}{shape}{qs}{' CONST' if isconst else ' (act)'}"

def main(path, wanted):
    model = load(path)
    sg = model.Subgraphs(0)
    n = sg.OperatorsLength()
    print(f"==== {path} ({n} ops)")
    for k in range(n):
        op = sg.Operators(k)
        name = opname(model, op)
        if name in wanted:
            oin = [op.Inputs(i) for i in range(op.InputsLength())]
            oout = [op.Outputs(i) for i in range(op.OutputsLength())]
            print(f"node {k}/{n}: {name}")
            for ti in oin: print("    in ", tinfo(model, sg, ti))
            for to in oout: print("    out", tinfo(model, sg, to))

if __name__ == "__main__":
    path = sys.argv[1]
    wanted = set(sys.argv[2].split(",")) if len(sys.argv) > 2 else {"TRANSPOSE_CONV"}
    main(path, wanted)
