"""Extrai todas as métricas dos arquivos brutos em docs/reports/ para metricas.json.

Nenhum número dos relatórios é digitado à mão: todos saem daqui.
Fontes:
  jacoco/<servico>/jacoco.csv        -> Branch Coverage, instruções, linhas
  pitest/<servico>/mutations.xml     -> Mutation Score, Test Strength, sobreviventes
  surefire/<servico>/TEST-*.xml      -> contagem de testes, ArchUnit
  ck/<servico>/class.csv             -> CBO

Uso (na raiz do repositório):  python docs/reports/extrair_metricas.py
"""
import collections
import csv
import glob
import hashlib
import json
import os
import xml.etree.ElementTree as ET

BASE = os.path.dirname(os.path.abspath(__file__))
SERVICOS = ["user-service", "product-service", "order-service", "report-service"]
DETECTADOS = {"KILLED", "TIMED_OUT", "MEMORY_ERROR", "RUN_ERROR"}


def camada(pacote, raiz):
    resto = pacote[len(raiz):].lstrip(".")
    return resto.split(".")[0] if resto else "(raiz)"


def jacoco(servico):
    arq = os.path.join(BASE, "jacoco", servico, "jacoco.csv")
    linhas = list(csv.DictReader(open(arq, encoding="utf-8")))
    tot = collections.Counter()
    por_pacote = collections.defaultdict(collections.Counter)
    abertos = []
    raiz = "com.tcc." + servico.split("-")[0]
    for r in linhas:
        for k in ("INSTRUCTION", "BRANCH", "LINE"):
            for m in ("MISSED", "COVERED"):
                v = int(r[f"{k}_{m}"])
                tot[f"{k}_{m}"] += v
                por_pacote[camada(r["PACKAGE"], raiz)][f"{k}_{m}"] += v
        if int(r["BRANCH_MISSED"]):
            abertos.append({"classe": r["CLASS"], "pacote": camada(r["PACKAGE"], raiz),
                            "cobertos": int(r["BRANCH_COVERED"]),
                            "total": int(r["BRANCH_COVERED"]) + int(r["BRANCH_MISSED"])})
    md5 = hashlib.md5(open(arq, "rb").read()).hexdigest().upper()
    return {"total": dict(tot), "por_pacote": {k: dict(v) for k, v in sorted(por_pacote.items())},
            "ramos_abertos": abertos, "md5": md5, "fonte": os.path.relpath(arq, BASE)}


def pitest(servico):
    arq = os.path.join(BASE, "pitest", servico, "mutations.xml")
    raiz = "com.tcc." + servico.split("-")[0]
    muts = []
    for x in ET.parse(arq).getroot():
        cls = x.findtext("mutatedClass")
        muts.append({"classe": cls.split(".")[-1], "pacote": camada(cls.rsplit(".", 1)[0], raiz),
                     "metodo": x.findtext("mutatedMethod"), "linha": int(x.findtext("lineNumber")),
                     "mutador": x.findtext("mutator").split(".")[-1], "descricao": x.findtext("description"),
                     "status": x.get("status")})
    st = collections.Counter(m["status"] for m in muts)
    mortos = sum(st[s] for s in DETECTADOS)
    por_pacote = collections.defaultdict(collections.Counter)
    for m in muts:
        por_pacote[m["pacote"]]["total"] += 1
        por_pacote[m["pacote"]]["mortos" if m["status"] in DETECTADOS else m["status"]] += 1
    return {"total": len(muts), "mortos": mortos, "sobreviventes": st["SURVIVED"],
            "sem_cobertura": st["NO_COVERAGE"], "status": dict(st),
            "por_pacote": {k: dict(v) for k, v in sorted(por_pacote.items())},
            "residuo": [m for m in muts if m["status"] in ("SURVIVED", "NO_COVERAGE")],
            "fonte": os.path.relpath(arq, BASE)}


def surefire(servico):
    suites = []
    for arq in sorted(glob.glob(os.path.join(BASE, "surefire", servico, "TEST-*.xml"))):
        r = ET.parse(arq).getroot()
        suites.append({"classe": r.get("name").split(".")[-1], "pacote": r.get("name").rsplit(".", 2)[-2],
                       "testes": int(r.get("tests")), "falhas": int(r.get("failures")),
                       "erros": int(r.get("errors")), "ignorados": int(r.get("skipped")),
                       "tempo": float(r.get("time")),
                       "casos": [c.get("name") for c in r.findall("testcase")]})
    return suites


def ck(servico):
    arq = os.path.join(BASE, "ck", servico, "class.csv")
    linhas = list(csv.DictReader(open(arq, encoding="utf-8")))
    raiz = "com.tcc." + servico.split("-")[0]
    return [{"classe": r["class"].split(".")[-1], "tipo": r["type"],
             "pacote": camada(r["class"].rsplit(".", 1)[0], raiz) if "." in r["class"] else "(raiz)",
             "cbo": int(r["cbo"])} for r in linhas]


def main():
    dados = {"servicos": {}}
    for s in SERVICOS:
        dados["servicos"][s] = {"jacoco": jacoco(s), "pitest": pitest(s),
                                "surefire": surefire(s), "ck": ck(s)}
    saida = os.path.join(BASE, "metricas.json")
    json.dump(dados, open(saida, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    for s, d in dados["servicos"].items():
        j, p = d["jacoco"]["total"], d["pitest"]
        print(f"{s:16} testes={sum(x['testes'] for x in d['surefire'])} "
              f"ramos={j['BRANCH_COVERED']}/{j['BRANCH_COVERED'] + j['BRANCH_MISSED']} "
              f"pit={p['mortos']}/{p['total']} md5={d['jacoco']['md5'][:8]}")
    print("->", os.path.relpath(saida))


if __name__ == "__main__":
    main()
