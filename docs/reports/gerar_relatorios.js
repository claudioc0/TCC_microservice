// Gera os 4 relatórios .docx dos microsserviços a partir de docs/reports/metricas.json.
// Uso: npm install docx@9  &&  node docs/reports/gerar_relatorios.js <raiz do repositório>
// Padrão visual copiado dos relatórios do monólito (Calibri 11, Letter, margens ABNT,
// cabeçalho de tabela F2F2F2, código em Consolas, rodapé "métrica | ferramenta | sistema | página").
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, WidthType, ShadingType,
  AlignmentType, HeadingLevel, BorderStyle, Footer, PageNumber, LevelFormat, TableLayoutType,
} = require("docx");

const REPO = process.argv[2];
const OUT = path.join(REPO, "docs", "reports");
const D = JSON.parse(fs.readFileSync(path.join(OUT, "metricas.json"), "utf8")).servicos;
const SV = Object.keys(D);
const CURTO = { "user-service": "user", "product-service": "product", "order-service": "order", "report-service": "report" };

// ---------------------------------------------------------------- dados do monólito
// Reconferidos a partir dos arquivos brutos do repositório TCC_monolith, commit 1c05487:
// docs/reports/jacoco/jacoco.csv, docs/reports/pitest/mutations.xml, docs/reports/ck/class.csv
const MONO = {
  testes: 98, ramos: [34, 42], linhas: [407, 450], instr: [1761, 1953],
  pit: { total: 170, mortos: 137, sobrev: 6, semcob: 27 }, pitSemConfig: [126, 127],
  arch: { regras: 7, violacoes: 0, historicas: 11 },
  cbo: { semAnon: [29, 7.45], soClass: [24, 8.46], todos: [34, 6.5] },
};

// ---------------------------------------------------------------- formatação numérica
const n = (x) => x.toLocaleString("pt-BR");
const pct = (a, b, d = 1) => (100 * a / b).toFixed(d).replace(".", ",") + "%";
const frac = (a, b) => `${n(a)}/${n(b)} = ${pct(a, b)}`;
const dec = (x, d = 2) => x.toFixed(d).replace(".", ",");
const sum = (arr) => arr.reduce((a, b) => a + b, 0);

// ---------------------------------------------------------------- blocos de texto
const W = 9405; // largura útil em DXA (12240 - 1701 - 1134)
const BORDA = { style: BorderStyle.SINGLE, size: 4, color: "BFBFBF" };

function runs(texto, base = {}) {
  // **negrito** e `código` inline
  const out = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let i = 0, m;
  while ((m = re.exec(texto))) {
    if (m.index > i) out.push(new TextRun({ text: texto.slice(i, m.index), ...base }));
    const t = m[0];
    if (t.startsWith("**")) out.push(new TextRun({ text: t.slice(2, -2), bold: true, ...base }));
    else out.push(new TextRun({ text: t.slice(1, -1), font: "Consolas", size: (base.size || 22) - 2, ...base }));
    i = m.index + t.length;
  }
  if (i < texto.length) out.push(new TextRun({ text: texto.slice(i), ...base }));
  return out;
}
const P = (t, o = {}) => new Paragraph({ alignment: AlignmentType.JUSTIFIED, spacing: { after: 160 }, ...o, children: runs(t) });
const H1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const H2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const B = (t) => new Paragraph({ numbering: { reference: "bullets", level: 0 }, alignment: AlignmentType.JUSTIFIED, spacing: { after: 80 }, children: runs(t) });
const CODE = (linhas) => linhas.map((l, i) => new Paragraph({
  shading: { type: ShadingType.CLEAR, color: "auto", fill: "F2F2F2" },
  spacing: { after: i === linhas.length - 1 ? 200 : 0 },
  children: [new TextRun({ text: l || " ", font: "Consolas", size: 18 })],
}));

class Doc {
  constructor() { this.filhos = []; this.tab = 0; }
  add(...x) { this.filhos.push(...x.flat()); return this; }
  tabela(cab, linhas, larguras, legenda, alinhar = []) {
    const total = sum(larguras);
    const ws = larguras.map((w) => Math.round(w * W / total));
    ws[ws.length - 1] += W - sum(ws);
    const cel = (t, j, cabecalho, destaque) => new TableCell({
      width: { size: ws[j], type: WidthType.DXA },
      shading: cabecalho ? { type: ShadingType.CLEAR, color: "auto", fill: "F2F2F2" } : undefined,
      margins: { top: 40, bottom: 40, left: 80, right: 80 },
      borders: { top: BORDA, bottom: BORDA, left: BORDA, right: BORDA },
      children: [new Paragraph({
        alignment: alinhar[j] === "r" ? AlignmentType.RIGHT : alinhar[j] === "c" ? AlignmentType.CENTER : AlignmentType.LEFT,
        children: runs(String(t), { size: 19, bold: cabecalho || destaque || undefined }),
      })],
    });
    const rows = [new TableRow({ tableHeader: true, children: cab.map((t, j) => cel(t, j, true)) })];
    for (const l of linhas) {
      const destaque = l.destaque;
      const vals = destaque ? l.valores : l;
      rows.push(new TableRow({ children: vals.map((t, j) => cel(t, j, false, destaque)) }));
    }
    this.add(new Table({ width: { size: W, type: WidthType.DXA }, columnWidths: ws, layout: TableLayoutType.FIXED, rows }));
    this.tab += 1;
    this.add(new Paragraph({ spacing: { before: 60, after: 240 }, children: [new TextRun({ text: `Tabela ${this.tab} – ${legenda}`, italics: true, size: 18 })] }));
  }
}
const destaque = (valores) => ({ destaque: true, valores });

function capa(titulo, subtitulo, meta) {
  const c = (t, size, o = {}) => new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 40 }, ...o, children: [new TextRun({ text: t, size, ...(o.run || {}) })] });
  const d = new Doc();
  d.add(
    c("Pontifícia Universidade Católica do Paraná", 22),
    c("Trabalho de Conclusão de Curso em Engenharia de Software Experimental", 22, { spacing: { after: 480 } }),
    c(titulo, 38, { run: { bold: true }, spacing: { after: 120 } }),
    c(subtitulo, 24, { spacing: { after: 480 } }),
  );
  d.tabela(["Item", "Valor"], meta, [28, 72], "Identificação da medição.");
  return d;
}

const META_COMUM = [
  ["Sistema medido", "TCC_microservice: user-service, product-service, order-service, report-service"],
  ["Código medido", "`main`, commit `8e6ce9d`"],
  ["Ambiente", "Eclipse Temurin 21.0.12.1 · Maven 3.9.16 · Spring Boot 3.2.0 · Windows 10"],
  ["Data da coleta", "19/10/2026 (arquivos brutos), referência de 30/09/2026"],
  ["Verificações independentes", "3 máquinas, resultado idêntico (seção de determinismo)"],
];

function salvar(doc, arquivo, rodape) {
  const document = new Document({
    creator: "TCC PUCPR — Microsserviços",
    title: arquivo.replace(".docx", ""),
    styles: {
      default: { document: { run: { font: "Calibri", size: 22 } } },
      paragraphStyles: [
        { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 28, bold: true, font: "Calibri" }, paragraph: { spacing: { before: 280, after: 120 }, outlineLevel: 0 } },
        { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
          run: { size: 24, bold: true, font: "Calibri" }, paragraph: { spacing: { before: 240, after: 120 }, outlineLevel: 1 } },
      ],
    },
    numbering: { config: [{ reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] }] },
    sections: [{
      properties: { page: { size: { width: 12240, height: 15840 }, margin: { top: 1417, right: 1134, bottom: 1417, left: 1701 } } },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [
        new TextRun({ text: `${rodape}   |   página `, size: 18 }), new TextRun({ children: [PageNumber.CURRENT], size: 18 })] })] }) },
      children: doc.filhos,
    }],
  });
  return Packer.toBuffer(document).then((b) => { fs.writeFileSync(path.join(OUT, arquivo), b); console.log("ok", arquivo); });
}

// ---------------------------------------------------------------- agregações
const J = (s) => D[s].jacoco.total;
const tot = (k) => sum(SV.map((s) => J(s)[k]));
const RAMOS = [tot("BRANCH_COVERED"), tot("BRANCH_COVERED") + tot("BRANCH_MISSED")];
const LINHAS = [tot("LINE_COVERED"), tot("LINE_COVERED") + tot("LINE_MISSED")];
const INSTR = [tot("INSTRUCTION_COVERED"), tot("INSTRUCTION_COVERED") + tot("INSTRUCTION_MISSED")];
const PIT = { total: sum(SV.map((s) => D[s].pitest.total)), mortos: sum(SV.map((s) => D[s].pitest.mortos)),
  sobrev: sum(SV.map((s) => D[s].pitest.sobreviventes)), semcob: sum(SV.map((s) => D[s].pitest.sem_cobertura)) };
const TESTES = sum(SV.map((s) => sum(D[s].surefire.map((x) => x.testes))));
const CONFIG = ["SecurityConfig", "ApplicationConfig", "RestClientConfig", "DatabaseSeeder"];
const RESIDUO = SV.flatMap((s) => D[s].pitest.residuo.map((m) => ({ ...m, servico: s })));
const semConfig = (() => {
  // mortos e alcançados fora das classes de configuração/inicialização
  // nenhum mutante dessas classes é morto ou sem cobertura (conferido em mutations.xml),
  // então basta tirar os sobreviventes delas dos alcançados
  const fora = RESIDUO.filter((m) => CONFIG.includes(m.classe.split("$")[0]) && m.status === "SURVIVED").length;
  return [PIT.mortos, PIT.total - PIT.semcob - fora];
})();
const porCamada = (fn) => {
  const acc = {};
  for (const s of SV) for (const [k, c] of Object.entries(fn(s))) {
    acc[k] = acc[k] || {};
    for (const [kk, v] of Object.entries(c)) acc[k][kk] = (acc[k][kk] || 0) + v;
  }
  return acc;
};
const NOME_CAMADA = (k) => k === "(raiz)" ? "(raiz: `*Application`, `DatabaseSeeder`)" : k;
const CK = (s) => D[s].ck.filter((c) => c.tipo !== "anonymous");
const CKALL = SV.flatMap(CK);
const media = (l) => sum(l.map((c) => c.cbo)) / l.length;

const tipoTeste = (x) => {
  if (x.pacote === "architecture") return "Arquitetura (ArchUnit)";
  if (/IntegrationTest$/.test(x.classe)) return "Integração (`@SpringBootTest`)";
  if (/ControllerTest$/.test(x.classe)) return "Controller (`@WebMvcTest`)";
  if (/ClientTest$/.test(x.classe)) return "Cliente HTTP (`MockRestServiceServer`)";
  return "Unidade (JUnit 5 + Mockito)";
};

const DETERMINISMO = [
  ["Referência (Claudio)", "30/09/2026", "Temurin 21.0.12.1", "`cb92d7b`", "171", "63/64", "174/210"],
  ["Vinicius", "09/10/2026", "não informado", "não informado", "171", "63/64", "174/210"],
  ["Eric", "16/10/2026", "Oracle 21.0.9", "`8e6ce9d`", "171", "63/64", "174/210"],
  ["Arquivos brutos do repositório", "19/10/2026", "Temurin 21.0.12.1", "`8e6ce9d`", String(TESTES), `${RAMOS[0]}/${RAMOS[1]}`, `${PIT.mortos}/${PIT.total}`],
];
const tabelaDeterminismo = (d) => d.tabela(["Execução", "Data", "JDK", "Commit", "Testes", "Ramos", "Mortos"], DETERMINISMO,
  [24, 13, 17, 13, 9, 10, 11], "Execuções independentes com `rodar-testes.bat` (3× testes, 2× PITest em cada uma).", ["", "", "", "", "r", "r", "r"]);

// ================================================================ 1. JaCoCo
function relatorioJacoco() {
  const d = capa("Relatório de Cobertura de Código", "Versão em microsserviços do experimento: medição com JaCoCo", [
    ...META_COMUM.slice(0, 2), ["Ferramenta", "JaCoCo 0.8.11 (jacoco-maven-plugin), relatório na fase `test`"],
    ["Comando", "`mvn clean test` em cada serviço"], ...META_COMUM.slice(2),
    ["Arquivos brutos", "`docs/reports/jacoco/<servico>/jacoco.csv` e `jacoco.xml`"]]);
  d.add(H1("1. O que este relatório mede"),
    P("Branch coverage é a fração dos desvios de fluxo (cada lado de um `if`, cada caso de um `switch`, cada operando de `&&` e `||`) que a suíte de testes executa pelo menos uma vez. É a métrica de adequação da suíte no protocolo GQM do experimento: responde se os testes chegam a passar por todas as decisões do código, e não se verificam o resultado delas. Essa segunda pergunta é a do relatório de mutation score."),
    P("O relatório também traz cobertura de linhas e de instruções de bytecode, porque elas ajudam a ler o resultado. O número de referência para a comparação com o monólito é sempre o de ramos."),
    H1("2. Como a medição foi feita"),
    P("Cada um dos quatro serviços é um módulo Maven independente, com o próprio `pom.xml`. O plugin JaCoCo está declarado igual nos quatro, na mesma versão do monólito:"),
    CODE(["<plugin>", "  <groupId>org.jacoco</groupId>", "  <artifactId>jacoco-maven-plugin</artifactId>", "  <version>0.8.11</version>",
      "  <executions>", "    <execution><goals><goal>prepare-agent</goal></goals></execution>",
      "    <execution><id>report</id><phase>test</phase><goals><goal>report</goal></goals></execution>", "  </executions>", "</plugin>"]),
    P("Os números saem das colunas `BRANCH_MISSED` e `BRANCH_COVERED` do `jacoco.csv` de cada serviço, somadas sobre todas as classes. É o mesmo método usado no monólito depois da correção que trocou as colunas de instrução pelas de ramo. Nenhuma classe foi excluída do relatório."),
    H1("3. Resultado geral"));
  d.tabela(["Contador", "Cobertos", "Total", "Não cobertos", "Cobertura"], [
    destaque(["Ramos (branches)", n(RAMOS[0]), n(RAMOS[1]), n(RAMOS[1] - RAMOS[0]), pct(...RAMOS)]),
    ["Linhas", n(LINHAS[0]), n(LINHAS[1]), n(LINHAS[1] - LINHAS[0]), pct(...LINHAS)],
    ["Instruções (bytecode)", n(INSTR[0]), n(INSTR[1]), n(INSTR[1] - INSTR[0]), pct(...INSTR)]],
  [30, 15, 15, 18, 15], "Contadores do JaCoCo somados nos quatro serviços.", ["", "r", "r", "r", "r"]);
  d.tabela(["Serviço", "Ramos", "Linhas", "Instruções"], [
    ...SV.map((s) => { const t = J(s); return [s, frac(t.BRANCH_COVERED, t.BRANCH_COVERED + t.BRANCH_MISSED), frac(t.LINE_COVERED, t.LINE_COVERED + t.LINE_MISSED), frac(t.INSTRUCTION_COVERED, t.INSTRUCTION_COVERED + t.INSTRUCTION_MISSED)]; }),
    destaque(["Agregado", frac(...RAMOS), frac(...LINHAS), frac(...INSTR)])],
  [22, 26, 26, 26], "Cobertura por serviço.", ["", "r", "r", "r"]);
  const cam = porCamada((s) => D[s].jacoco.por_pacote);
  const ordem = Object.keys(cam).sort((a, b) => (cam[b].BRANCH_COVERED + cam[b].BRANCH_MISSED) - (cam[a].BRANCH_COVERED + cam[a].BRANCH_MISSED));
  d.add(H1("4. Cobertura por camada"),
    P("Agregando as camadas equivalentes dos quatro serviços (os nomes de pacote são os mesmos em todos):"));
  d.tabela(["Camada", "Ramos", "% ramos", "Linhas", "% linhas"], ordem.map((k) => {
    const c = cam[k], rb = c.BRANCH_COVERED + c.BRANCH_MISSED, rl = c.LINE_COVERED + c.LINE_MISSED;
    return [NOME_CAMADA(k), rb ? `${c.BRANCH_COVERED}/${rb}` : "—", rb ? pct(c.BRANCH_COVERED, rb) : "—", `${c.LINE_COVERED}/${rl}`, pct(c.LINE_COVERED, rl)];
  }), [34, 14, 14, 18, 14], "Cobertura por camada. Camadas sem ramos não têm lógica condicional.", ["", "r", "r", "r", "r"]);
  const ab = SV.flatMap((s) => D[s].jacoco.ramos_abertos.map((r) => ({ ...r, s })));
  d.add(H1("5. Onde está o ramo que a suíte não alcança"));
  d.tabela(["Serviço", "Classe", "Ramos cobertos", "Ramos em aberto"], ab.map((r) => [r.s, r.classe, `${r.cobertos}/${r.total}`, String(r.total - r.cobertos)]),
    [22, 34, 22, 22], "Classes com ramos não exercitados.", ["", "", "r", "r"]);
  d.add(P("É um único ramo, em `OrderController.findAll`, linha 62:"),
    CODE(["if (startDate != null && endDate != null) {", "    return ResponseEntity.ok(orderService.findByPeriod(startDate, endDate));", "}", "return ResponseEntity.ok(orderService.findAll());"]),
    P("Os testes chamam `GET /api/orders` sem datas e com as duas datas, mas nunca com só uma delas. Esse é o caso `startDate != null` e `endDate == null`, que o JaCoCo conta como ramo em aberto. É teste faltando, de baixo risco: o comportamento esperado é cair na listagem completa."),
    P("O relatório anterior atribuía esse resíduo a `ProductServiceUnavailableException`, em cenários de rede não simulados. A leitura do relatório HTML do JaCoCo mostra que não é o caso, e a explicação foi corrigida."),
    H1("6. Por que a cobertura de ramos discrimina pouco neste projeto"),
    P(`O sistema tem ${RAMOS[1]} ramos em ${n(LINHAS[1])} linhas executáveis. A maior parte do código (DTOs, controllers, repositórios, configuração) não tem decisão nenhuma, e os ramos que existem se concentram em segurança (${cam.security.BRANCH_COVERED + cam.security.BRANCH_MISSED}), serviço (${cam.service.BRANCH_COVERED + cam.service.BRANCH_MISSED}) e entidades (${cam.entity.BRANCH_COVERED + cam.entity.BRANCH_MISSED}). Com tão poucos ramos, um único teste a mais ou a menos mexe vários pontos percentuais. Por isso a métrica é lida junto com o mutation score, que distingue código executado de código verificado.`),
    P(`Há também uma diferença de escala em relação ao monólito: ${RAMOS[1]} ramos aqui contra ${MONO.ramos[1]} lá. Os filtros JWT, os handlers de erro e os clientes HTTP existem em várias cópias, uma por serviço, e cada cópia traz seus próprios ramos. Comparar só os percentuais esconde que o sistema decomposto tem mais decisões para testar.`),
    H1("7. Determinismo da medição"));
  d.tabela(["Serviço", "MD5 do jacoco.csv", "Igual nas 3 rodadas e nas 3 máquinas"], SV.map((s) => [s, "`" + D[s].jacoco.md5 + "`", "sim"]),
    [22, 52, 26], "Hash do jacoco.csv de cada serviço.", ["", "", "c"]);
  tabelaDeterminismo(d);
  d.add(P("O `jacoco.csv` saiu idêntico byte a byte nas três rodadas de cada execução e entre as máquinas, inclusive com outro fornecedor e outra versão de JDK (Oracle 21.0.9). A cobertura é determinística: nenhum teste depende de relógio, rede, ordem de execução ou estado de banco persistido."),
    H1("8. Comparação com o monólito"));
  d.tabela(["Métrica", "Monólito", "Microsserviços"], [
    ["Testes", String(MONO.testes), String(TESTES)],
    destaque(["Cobertura de ramos", frac(...MONO.ramos), frac(...RAMOS)]),
    ["Cobertura de linhas", frac(...MONO.linhas), frac(...LINHAS)],
    ["Cobertura de instruções", frac(...MONO.instr), frac(...INSTR)],
    ["Total de ramos no sistema", String(MONO.ramos[1]), String(RAMOS[1])],
    ["JaCoCo / JDK", "0.8.11 / 21.0.12.1", "0.8.11 / 21.0.12.1"]],
  [34, 33, 33], "Comparação nas mesmas versões de ferramenta e JDK. Monólito reconferido no jacoco.csv do commit 1c05487.", ["", "r", "r"]);
  d.add(P(`A cobertura de ramos dos microsserviços é maior (${pct(...RAMOS)} contra ${pct(...MONO.ramos)}), mas essa diferença **não pode ser atribuída só à arquitetura**. Os microsserviços têm ${TESTES} testes contra ${MONO.testes}, incluindo 34 testes de controller com \`@WebMvcTest\` que o monólito não tem. Os ramos em aberto do monólito estão justamente nas camadas que esse tipo de teste exercita. A diferença de esforço de teste é a principal ameaça à validade desta comparação.`),
    H1("9. Como reproduzir"),
    CODE([":: Windows, na raiz do repositório: 3x mvn clean test + 2x PITest, gera resumo.txt", "rodar-testes.bat", "",
      ":: manualmente, por serviço", "cd <servico>", "mvn clean test", ":: relatório navegável: target/site/jacoco/index.html", ":: dados brutos:       target/site/jacoco/jacoco.csv"]),
    P("Para recalcular as tabelas deste relatório a partir dos arquivos versionados: `python docs/reports/extrair_metricas.py`."));
  return salvar(d, "Relatorio-Cobertura-Codigo-JaCoCo.docx", "Cobertura de código | JaCoCo 0.8.11 | microsserviços");
}

// ================================================================ 2. PITest
function categoria(m) {
  const k = `${m.classe}.${m.metodo}`;
  if (m.status === "NO_COVERAGE") return null;
  if (k.startsWith("SecurityConfig.corsConfigurationSource")) return "CORS";
  if (k.startsWith("SecurityConfig.securityFilterChain")) return "Cadeia de filtros";
  if (k.startsWith("SecurityConfig.passwordEncoder")) return "Encoder de senha";
  if (k.startsWith("GlobalExceptionHandler$ErrorResponse")) return "Corpo da resposta de erro";
  if (m.classe === "DatabaseSeeder") return "Seeder";
  if (m.classe === "RestClientConfig") return "Configuração HTTP";
  return "DTO de resposta";
}
const MOTIVO = {
  "CORS": ["`SecurityConfig.corsConfigurationSource` (4 por serviço)", "Nenhum teste verifica os headers CORS da resposta."],
  "Cadeia de filtros": ["`SecurityConfig.securityFilterChain` (1 por serviço)", "O mutante devolve `null` para o bean; o mesmo caso existe no monólito."],
  "Encoder de senha": ["`SecurityConfig.passwordEncoder` (user-service)", "O mutante devolve `null` para o `@Bean`."],
  "Corpo da resposta de erro": ["`GlobalExceptionHandler$ErrorResponse.of` (1 por serviço)", "Os testes conferem o status HTTP do erro, não o corpo JSON."],
  "Seeder": ["`DatabaseSeeder.seedUsers` e sua lambda (user-service)", "Mesma categoria de 3 dos 6 sobreviventes do monólito."],
  "Configuração HTTP": ["`RestClientConfig.restClientBuilder` (order-service)", "Wiring de framework."],
  "DTO de resposta": ["`OrderResponse$OrderItemResponse.from` (order-service)", "Nenhum teste inspeciona os itens na resposta do pedido."],
};
function relatorioPitest() {
  const d = capa("Relatório de Mutation Score", "Versão em microsserviços do experimento: medição com PITest", [
    ...META_COMUM.slice(0, 2), ["Ferramenta", "PITest 1.15.3 com pitest-junit5-plugin 1.2.1, mutadores DEFAULTS"],
    ["Comando", "`mvn org.pitest:pitest-maven:mutationCoverage` em cada serviço, depois de `mvn clean test`"], ...META_COMUM.slice(2),
    ["Arquivos brutos", "`docs/reports/pitest/<servico>/mutations.xml`"]]);
  const TS = [PIT.mortos, PIT.total - PIT.semcob];
  d.add(H1("1. O que este relatório mede"),
    P("Cobertura diz se a linha rodou, não se algum teste olhou o resultado. O teste de mutação ataca esse ponto cego: o PITest planta defeitos pequenos e propositais no bytecode já compilado (inverte uma condição, faz um método devolver `null`, apaga uma chamada) e roda a suíte contra cada um. Se algum teste falha, o mutante está **morto** (KILLED). Se todos passam, ele **sobreviveu** (SURVIVED)."),
    P("Há um terceiro estado, **sem cobertura** (NO_COVERAGE), para mutantes em código que nenhum teste executa. Separar esse estado é o que permite as duas métricas deste relatório: o **mutation score** (mortos sobre o total) e o **test strength** (mortos sobre os alcançados, excluindo os sem cobertura). O primeiro mistura alcance e força das asserções; o segundo isola a força."),
    H1("2. Configuração da medição"),
    P("O plugin está declarado igual nos quatro `pom.xml`, com `<servico>` trocado pelo nome de cada um:"),
    CODE(["<plugin>", "  <groupId>org.pitest</groupId>", "  <artifactId>pitest-maven</artifactId>", "  <version>1.15.3</version>",
      "  <dependencies>", "    <dependency>org.pitest:pitest-junit5-plugin:1.2.1</dependency>", "  </dependencies>", "  <configuration>",
      "    <targetClasses><param>com.tcc.<servico>.*</param></targetClasses>", "    <targetTests><param>com.tcc.<servico>.*</param></targetTests>",
      "    <excludedTestClasses><param>com.tcc.<servico>.architecture.*</param></excludedTestClasses>",
      "    <mutators><mutator>DEFAULTS</mutator></mutators>", "    <timestampedReports>false</timestampedReports>", "  </configuration>", "</plugin>"]),
    P("Esse é o mesmo critério do monólito: o pacote inteiro como alvo, todos os testes no escopo exceto os de arquitetura, e nenhuma classe de produção excluída. A seção 8 explica por que a configuração era outra até 30/09/2026 e o efeito da mudança."),
    H1("3. Resultado geral"),
    P(`O PITest gerou ${PIT.total} mutantes nas classes de produção dos quatro serviços e rodou a suíte de ${TESTES} testes contra cada um deles.`));
  d.tabela(["Estado do mutante", "Qtd.", "% do total"], [
    ["Mortos (KILLED)", String(PIT.mortos), pct(PIT.mortos, PIT.total)], ["Sobreviventes (SURVIVED)", String(PIT.sobrev), pct(PIT.sobrev, PIT.total)],
    ["Sem cobertura (NO_COVERAGE)", String(PIT.semcob), pct(PIT.semcob, PIT.total)], destaque(["Total", String(PIT.total), "100,0%"])],
  [50, 20, 30], "Distribuição dos mutantes por estado, quatro serviços.", ["", "r", "r"]);
  d.tabela(["Métrica", "Cálculo", "Valor"], [
    destaque(["Mutation score", `mortos ÷ total = ${PIT.mortos} ÷ ${PIT.total}`, pct(PIT.mortos, PIT.total)]),
    destaque(["Test strength", `mortos ÷ alcançados = ${TS[0]} ÷ ${TS[1]}`, pct(...TS)])],
  [28, 50, 22], "Mutation score e test strength.", ["", "", "r"]);
  d.add(H1("4. Resultado por serviço"));
  d.tabela(["Serviço", "Mutantes", "Mortos", "Sobrev.", "Sem cob.", "Score", "Test strength"], [
    ...SV.map((s) => { const p = D[s].pitest; return [s, String(p.total), String(p.mortos), String(p.sobreviventes), String(p.sem_cobertura), pct(p.mortos, p.total), pct(p.mortos, p.total - p.sem_cobertura)]; }),
    destaque(["Agregado", String(PIT.total), String(PIT.mortos), String(PIT.sobrev), String(PIT.semcob), pct(PIT.mortos, PIT.total), pct(...TS)])],
  [22, 12, 11, 11, 11, 13, 20], "Mutation score por serviço.", ["", "r", "r", "r", "r", "r", "r"]);
  const cam = porCamada((s) => D[s].pitest.por_pacote);
  const ordem = Object.keys(cam).sort((a, b) => ((cam[b].mortos || 0) / cam[b].total) - ((cam[a].mortos || 0) / cam[a].total) || cam[b].total - cam[a].total);
  d.add(H1("5. Resultado por camada"));
  d.tabela(["Camada", "Mortos", "Sobrev.", "Sem cob.", "Total", "Score"], ordem.map((k) => {
    const c = cam[k]; return [NOME_CAMADA(k), String(c.mortos || 0), String(c.SURVIVED || 0), String(c.NO_COVERAGE || 0), String(c.total), pct(c.mortos || 0, c.total)];
  }), [34, 13, 13, 13, 12, 15], "Mutation score por camada, agregado dos quatro serviços.", ["", "r", "r", "r", "r", "r"]);
  d.add(P("Toda a regra de negócio está em 100%: nenhum mutante de `service` ou `entity` sobrevive ou fica sem cobertura. O resíduo está em configuração (`security`, `config`, raiz), no tratamento de erro (`exception`) e num DTO."),
    H1("6. Os mutantes sobreviventes"));
  const sob = RESIDUO.filter((m) => m.status === "SURVIVED");
  const cats = {};
  for (const m of sob) cats[categoria(m)] = (cats[categoria(m)] || 0) + 1;
  d.tabela(["Categoria", "Qtd.", "Classe e método", "Por que sobrevive"], Object.entries(cats).sort((a, b) => b[1] - a[1]).map(([c, q]) => [c, String(q), ...MOTIVO[c]]),
    [17, 7, 36, 40], `Os ${sob.length} sobreviventes, por categoria.`, ["", "r", "", ""]);
  const sec = sob.filter((m) => m.classe === "SecurityConfig").length;
  d.add(P(`**${sec} dos ${sob.length} sobreviventes estão na \`SecurityConfig\`, que existe copiada nos quatro serviços.** É um efeito estrutural da decomposição: no monólito há uma \`SecurityConfig\`; aqui há quatro, e a mesma lacuna de teste (nenhum teste verifica CORS nem que a cadeia de filtros é a customizada) conta quatro vezes.`),
    P("A lista completa, mutante a mutante:"));
  d.tabela(["Serviço", "Classe.método", "Linha", "Mutação aplicada"], sob.map((m) => [CURTO[m.servico], `${m.classe}.${m.metodo}`, String(m.linha), m.descricao.replace(/(for|to) [\w/$]*\/([\w$]+::)/, "$1 $2")]),
    [11, 41, 8, 40], "Mutantes sobreviventes, extraídos de mutations.xml.", ["", "", "r", ""]);
  d.add(H1("7. Os mutantes sem cobertura"));
  const nc = RESIDUO.filter((m) => m.status === "NO_COVERAGE");
  d.tabela(["Serviço", "Classe.método", "Linha", "Por quê"], nc.map((m) => [CURTO[m.servico], `${m.classe}.${m.metodo}`, String(m.linha),
    m.classe === "UserController" ? "Endpoint sem teste." : "Nenhum teste propaga esse erro até o handler HTTP."]),
  [11, 45, 8, 36], "Mutantes sem cobertura.", ["", "", "r", ""]);
  d.add(P("Quatro dos seis são caminhos de falha de **serviço remoto**: os três do order-service (estoque insuficiente, produto não encontrado e product-service indisponível, lançados pelo `ProductClient` a partir da resposta HTTP) e o order-service indisponível no report-service. Esses caminhos não existem no monólito. Os outros dois (`handleAccessDenied` e `UserController.findById`) são locais. A decomposição criou uma categoria de falha que a suíte não cobriu. Nenhum desses casos exige mudança em código de produção: é teste faltando."),
    H1("8. Linhagem: o escopo do PITest e a correção sobre o CGLIB"),
    P("Até 30/09/2026, o PITest dos microsserviços mutava só os pacotes `entity`, `service` e `security`, e o `targetTests` estava restrito aos testes desses mesmos pacotes. Os testes de controller e os quatro `@SpringBootTest` de integração não participavam. Com essa configuração, a medição dava 149 mutantes e test strength de 100%."),
    P("O monólito já mutava o pacote inteiro com todos os testes, e tinha removido as próprias exclusões por inflarem o score. A configuração dos microsserviços foi alinhada à do monólito (seção 2), e o resultado passou a ser o deste relatório."),
    P("O relatório anterior atribuía os mutantes sem cobertura da `SecurityConfig` a uma limitação do PITest com o proxy CGLIB das classes `@Configuration`. **Isso estava errado.** Com o escopo alinhado, os testes que sobem o contexto Spring passam a executar esses métodos, e os mutantes sobrevivem. O test strength de 100% era efeito do escopo restrito, não uma propriedade da suíte."));
  d.tabela(["Configuração", "Mutantes", "Mortos", "Sem cob.", "Test strength"], [
    ["Escopo restrito (até 30/09)", "149", "128", "21", "100%"], destaque(["Escopo alinhado ao monólito (atual)", String(PIT.total), String(PIT.mortos), String(PIT.semcob), pct(...TS)])],
  [40, 14, 14, 14, 18], "Efeito do alinhamento do escopo. A linha de escopo restrito vem do RESULTADOS_METRICAS.md de 13/09/2026.", ["", "r", "r", "r", "r"]);
  d.add(H1("9. Análise de sensibilidade: sem código de configuração"),
    P("Para separar o efeito do código de configuração replicado, a comparação foi refeita tirando da conta, dos dois lados, as classes de configuração e de inicialização (`SecurityConfig`, `ApplicationConfig`, `RestClientConfig`, `DatabaseSeeder`). O critério de exclusão é o mesmo nos dois lados."));
  d.tabela(["", "Test strength bruto", "Test strength sem configuração"], [
    ["Monólito", frac(MONO.pit.mortos, MONO.pit.total - MONO.pit.semcob), frac(...MONO.pitSemConfig)],
    ["Microsserviços", frac(...TS), frac(...semConfig)]],
  [24, 38, 38], "Sensibilidade do test strength ao código de configuração.", ["", "r", "r"]);
  d.add(P(`A diferença cai de ${dec(100 * MONO.pit.mortos / (MONO.pit.total - MONO.pit.semcob) - 100 * TS[0] / TS[1], 1)} para ${dec(100 * MONO.pitSemConfig[0] / MONO.pitSemConfig[1] - 100 * semConfig[0] / semConfig[1], 1)} pontos percentuais. A diferença bruta vem quase toda do código de configuração, não de testes mais fracos em regra de negócio.`),
    H1("10. Determinismo da medição"));
  tabelaDeterminismo(d);
  d.add(P("Em cada execução o PITest rodou duas vezes, e os dois `mutations.xml` foram comparados mutante a mutante (classe, método, linha, mutador e índice), não só nos totais: o conjunto de mutantes e o estado de cada um saíram idênticos. Trocar o JDK para Oracle 21.0.9 não mudou nem o total de mutantes gerados, que é sensível ao bytecode."),
    H1("11. Comparação com o monólito"));
  d.tabela(["Métrica", "Monólito", "Microsserviços"], [
    ["Mutantes gerados", String(MONO.pit.total), String(PIT.total)], ["Mortos", String(MONO.pit.mortos), String(PIT.mortos)],
    ["Sobreviventes", String(MONO.pit.sobrev), String(PIT.sobrev)], ["Sem cobertura", String(MONO.pit.semcob), String(PIT.semcob)],
    destaque(["Mutation score", frac(MONO.pit.mortos, MONO.pit.total), frac(PIT.mortos, PIT.total)]),
    destaque(["Test strength", frac(MONO.pit.mortos, MONO.pit.total - MONO.pit.semcob), frac(...TS)]),
    ["PITest / JDK", "1.15.3 / 21.0.12.1", "1.15.3 / 21.0.12.1"]],
  [34, 33, 33], "Comparação nas mesmas versões. Monólito reconferido no mutations.xml do commit 1c05487.", ["", "r", "r"]);
  d.add(P(`O mutation score é próximo nos dois lados (diferença de ${dec(100 * PIT.mortos / PIT.total - 100 * MONO.pit.mortos / MONO.pit.total, 1)} pontos). O monólito tem mais mutantes sem cobertura (${MONO.pit.semcob} contra ${PIT.semcob}), concentrados nos controllers que não têm teste de fatia web. Os microsserviços têm mais sobreviventes (${PIT.sobrev} contra ${MONO.pit.sobrev}), concentrados na configuração replicada. Nenhuma das duas diferenças está em regra de negócio.`),
    H1("12. Como reproduzir"),
    CODE([":: Windows, na raiz do repositório", "rodar-testes.bat", "", ":: manualmente, por serviço (o goal do PITest não compila: rode os testes antes)", "cd <servico>", "mvn clean test",
      "mvn org.pitest:pitest-maven:mutationCoverage", ":: relatório navegável: target/pit-reports/index.html", ":: dados brutos:       target/pit-reports/mutations.xml"]));
  return salvar(d, "Relatorio-Mutation-Score-PITest.docx", "Mutation score | PITest 1.15.3 | microsserviços");
}

// ================================================================ 3. ArchUnit
const REGRAS = [
  ["Controller não acessa repository", "controllersShouldNotAccessRepositoriesDirectly", "Nenhuma classe em `..controller..` depende de `..repository..`."],
  ["Controller não depende de entidade", "controllersShouldNotDependOnEntities", "Classes `@RestController` não dependem de classes `@Entity` (expõem DTOs)."],
  ["Convenção de nomes", "namingConventionShouldBeRespected", "`@RestController` termina em Controller, `@Service` em Service, `@Repository` em Repository."],
  ["Repository só é acessado por service", "repositoriesShouldOnlyBeAccessedByServices", "Classes de `..repository..` só são acessadas por `..service..`."],
  ["Cliente HTTP só é acessado por service", "onlyServiceShouldAccess*Client", "Classes de `..client..` só são acessadas por `..service..` e pelo próprio `..client..`."],
  ["Fronteira entre serviços", "shouldNotDependOnOtherServicesPackages", "Nenhuma classe do serviço depende de pacotes `com.tcc.<outro serviço>..`."],
];
function relatorioArchunit() {
  const d = capa("Relatório de Violações Arquiteturais", "Versão em microsserviços do experimento: verificação com ArchUnit", [
    ...META_COMUM.slice(0, 2), ["Ferramenta", "ArchUnit 1.2.1 (archunit-junit5), regras escritas como testes JUnit 5"],
    ["Comando", "`mvn clean test` em cada serviço (as regras fazem parte da suíte)"], ...META_COMUM.slice(2),
    ["Arquivos brutos", "`docs/reports/surefire/<servico>/TEST-com.tcc.<servico>.architecture.ArchitectureTest.xml`"]]);
  const arch = SV.map((s) => ({ s, x: D[s].surefire.find((t) => t.pacote === "architecture") }));
  const totalRegras = sum(arch.map((a) => a.x.testes));
  const viol = sum(arch.map((a) => a.x.falhas + a.x.erros));
  d.add(H1("1. O que este relatório mede"),
    P("A métrica de integridade estrutural do protocolo GQM é a densidade de violações arquiteturais: quantas regras de dependência entre camadas e entre domínios o código quebra. As regras são verificadas com ArchUnit, que importa o bytecode das classes de produção e checa as dependências entre elas."),
    H1("2. Configuração da verificação"),
    P("Cada serviço tem uma classe `ArchitectureTest` com as regras dele. As classes de produção são importadas uma única vez por suíte, sem as classes de teste:"),
    CODE(["@BeforeAll", "static void setup() {", "    importedClasses = new ClassFileImporter()", "            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)", "            .importPackages(\"com.tcc.<servico>\");", "}"]),
    P("Cada regra é um método `@Test` que chama `rule.check(importedClasses)`. Uma violação faz o teste falhar, e o Surefire a registra como falha no XML da suíte. As classes de arquitetura ficam fora do escopo do PITest (`excludedTestClasses`), como no monólito."),
    H1("3. Resultado"));
  d.tabela(["Serviço", "Regras", "Violações", "Tempo", "Resultado"], [
    ...arch.map((a) => [a.s, String(a.x.testes), String(a.x.falhas + a.x.erros), dec(a.x.tempo, 2) + " s", "passou"]),
    destaque(["Total", String(totalRegras), String(viol), "—", "verde"])],
  [26, 14, 16, 18, 26], "Resultado das suítes de arquitetura (atributos tests, failures, errors e time do XML do Surefire).", ["", "r", "r", "r", "c"]);
  d.add(P(`**Densidade de violações: ${viol} em ${totalRegras} regras = ${dec(viol / totalRegras, 1)}.** O resultado foi o mesmo nas três rodadas de cada uma das quatro execuções independentes.`),
    H1("4. As regras"),
    P("As 17 regras são 6 regras distintas, replicadas conforme o que cada serviço tem:"));
  d.tabela(["Regra", "Método de teste", "O que verifica"], REGRAS, [24, 34, 42], "Regras distintas e o que cada uma verifica.");
  const tem = (s, metodo) => D[s].surefire.find((t) => t.pacote === "architecture").casos.some((c) => metodo.endsWith("*Client") ? /^onlyServiceShouldAccess\w+Client$/.test(c) : c === metodo);
  d.tabela(["Regra", "user", "product", "order", "report"], REGRAS.map(([r, m]) => [r, ...SV.map((s) => tem(s, m) ? "sim" : "—")]),
    [36, 16, 16, 16, 16], "Quais regras existem em cada serviço (nomes dos testcase no XML do Surefire).", ["", "c", "c", "c", "c"]);
  d.add(P("O report-service não tem banco nem repositório, por isso não tem as regras de repository e de entidade. A regra de fronteira entre serviços existe no order-service e no report-service, os dois serviços que chamam outro serviço por HTTP e, portanto, os únicos em que uma dependência indevida seria tentadora."),
    H1("5. Leitura para o experimento: fronteira física contra disciplina"),
    P(`No monólito, a fronteira entre domínios depende de verificação ativa. As regras de ArchUnit já encontraram **${MONO.arch.historicas} violações reais** no código dele (entidade vazando entre domínios, acesso direto ao repositório de outro domínio), todas corrigidas. Zero violações lá significa que a disciplina da equipe se manteve até agora, e pode deixar de se manter no próximo commit.`),
    P("Nos microsserviços, a regra de fronteira não tem como falhar. A violação exigiria um import de uma classe que não existe no classpath, porque cada serviço é um módulo Maven separado, sem dependência dos outros. A fronteira é garantida pela estrutura, não pela regra."),
    P("Por isso o \"0 contra 0\" sozinho não diz nada. O resultado do experimento é que um lado precisa da regra para manter a fronteira, e o outro tem a fronteira garantida pela forma como foi construído."),
    H1("6. Comparação com o monólito"));
  d.tabela(["Item", "Monólito", "Microsserviços"], [
    ["Versão do ArchUnit", "1.2.1", "1.2.1"], ["Classes de regras", "2", "4 (uma por serviço)"],
    destaque(["Regras executadas", String(MONO.arch.regras), `${totalRegras} (6 distintas)`]), destaque(["Violações atuais", "0", String(viol)]),
    ["Violações históricas encontradas", String(MONO.arch.historicas), "0"], ["Fronteira entre domínios", "regra ArchUnit", "separação física em módulos"]],
  [34, 33, 33], "Paridade das suítes de arquitetura entre as duas versões.", ["", "r", "r"]);
  d.add(P("**A densidade de violações não é comparável diretamente:** os conjuntos de regras são diferentes (7 regras no monólito, 17 aqui), e uma parte das regras dos microsserviços se repete por serviço. Para uma comparação numérica, seria preciso alinhar as regras ou comparar só as equivalentes. Essa diferença está registrada como ameaça à validade em aberto."),
    H1("7. Como reproduzir"),
    CODE(["cd <servico>", "mvn clean test -Dtest=ArchitectureTest", ":: resultado: target/surefire-reports/TEST-com.tcc.<servico>.architecture.ArchitectureTest.xml"]));
  return salvar(d, "Relatorio-Violacoes-Arquiteturais-ArchUnit.docx", "Violações arquiteturais | ArchUnit 1.2.1 | microsserviços");
}

// ================================================================ 4. ck / CBO
function relatorioCk() {
  const d = capa("Relatório de Acoplamento (CBO)", "Versão em microsserviços do experimento: medição com ck", [
    ...META_COMUM.slice(0, 2), ["Ferramenta", "ck 0.7.0 (com.github.mauricioaniche:ck), sobre o código-fonte"],
    ["Comando", "`java -cp <ck e dependências> com.github.mauricioaniche.ck.Runner <servico>/src/main/java false 0 false <saída>/`"], ...META_COMUM.slice(2),
    ["Arquivos brutos", "`docs/reports/ck/<servico>/class.csv` e `method.csv`"]]);
  const todos = SV.flatMap((s) => D[s].ck), soClass = todos.filter((c) => c.tipo === "class");
  d.add(H1("1. O que este relatório mede"),
    P("CBO (Coupling Between Objects) é o número de outros tipos de que uma classe depende: campos, parâmetros, retornos, exceções, anotações e chamadas. É a métrica de acoplamento estrutural do protocolo GQM, usada como indicador do esforço de isolamento nos testes: quanto mais dependências uma classe tem, mais coisas precisam ser substituídas por mocks para testá-la sozinha."),
    H1("2. Como a medição foi feita"),
    P("O ck analisa o código-fonte, sem precisar compilar. Foi rodado uma vez por serviço, sobre `src/main/java` (só produção), na mesma versão usada no monólito:"),
    CODE(["java -cp \"<ck-0.7.0.jar e dependências>\" com.github.mauricioaniche.ck.Runner \\", "    <servico>/src/main/java false 0 false docs/reports/ck/<servico>/"]),
    P("O ck gera uma linha por tipo Java no `class.csv`, com o tipo na coluna `type` (`class`, `interface`, `enum` ou `anonymous`) e o CBO na coluna `cbo`. O código de produção não mudou desde a primeira medição de CBO, e os valores por classe são os mesmos."),
    H1("3. Critério de contagem: uma correção"),
    P("Os dois lados do experimento filtraram os tipos de forma diferente antes de calcular a média:"),
    B("**Monólito:** excluiu só as classes anônimas, que são as constantes do enum `OrderStatus` com método próprio, e manteve interfaces e enums."),
    B("**Relatório anterior de microsserviços:** manteve só `type = class`, ou seja, excluiu também os repositórios Spring Data (interfaces) e os enums."),
    P("A comparação publicada antes (7,45 no monólito contra 6,71 aqui) misturava os dois critérios. Calculado dos dois lados com cada critério:"));
  d.tabela(["Critério", "Monólito", "Microsserviços"], [
    destaque(["Sem classes anônimas (critério do monólito, adotado)", `${MONO.cbo.semAnon[0]} tipos · ${dec(MONO.cbo.semAnon[1])}`, `${CKALL.length} tipos · ${dec(media(CKALL))}`]),
    ["Só type = class (critério do relatório anterior)", `${MONO.cbo.soClass[0]} tipos · ${dec(MONO.cbo.soClass[1])}`, `${soClass.length} tipos · ${dec(media(soClass))}`],
    ["Todos os tipos", `${MONO.cbo.todos[0]} tipos · ${dec(MONO.cbo.todos[1])}`, `${todos.length} tipos · ${dec(media(todos))}`]],
  [46, 27, 27], "CBO médio sob cada critério. Monólito reconferido no class.csv do commit 1c05487.", ["", "r", "r"]);
  d.add(P(`A direção não muda em nenhum critério: o CBO médio dos microsserviços é menor. A magnitude muda. O valor de referência passa a ser **${dec(media(CKALL))}**, calculado com o mesmo critério do monólito.`),
    H1("4. Resultado por serviço"));
  d.tabela(["Serviço", "Tipos", "CBO médio", "CBO máximo", "Classe com CBO máximo"], [
    ...SV.map((s) => { const c = CK(s), m = c.reduce((a, b) => (b.cbo > a.cbo ? b : a)); return [s, String(c.length), dec(media(c)), String(m.cbo), m.classe]; }),
    destaque(["Agregado", String(CKALL.length), dec(media(CKALL)), "", ""])],
  [24, 12, 16, 16, 32], "CBO por serviço, sem classes anônimas.", ["", "r", "r", "r", ""]);
  const cam = {};
  for (const c of CKALL) (cam[c.pacote] = cam[c.pacote] || []).push(c);
  d.add(H1("5. Resultado por camada"));
  d.tabela(["Camada", "Tipos", "CBO médio"], Object.entries(cam).sort((a, b) => media(b[1]) - media(a[1])).map(([k, l]) => [NOME_CAMADA(k), String(l.length), dec(media(l))]),
    [50, 20, 30], "CBO por camada, agregado dos quatro serviços.", ["", "r", "r"]);
  d.add(P("Os controllers concentram o maior CBO médio, como no monólito: dependem dos DTOs de entrada e saída, do service e da infraestrutura de segurança (`AuthenticatedPrincipal`). As exceções têm CBO baixo por construção: são classes folha."),
    H1("6. Ressalva: o acoplamento entre domínios mudou de forma"),
    P("No monólito, uma chamada de `OrderService` para regras de produto é uma dependência Java direta, que o CBO conta. Na decomposição, a dependência de domínio continua existindo (o order-service precisa consultar e ajustar estoque), mas virou contrato HTTP. Dentro do processo, ela sobra em classes cliente dedicadas:"));
  const cli = SV.flatMap((s) => CK(s).filter((c) => c.pacote === "client" && /Client$/.test(c.classe)).map((c) => ({ ...c, s })));
  for (const c of cli) d.add(B(`\`${c.s}.${c.classe}\`: CBO ${c.cbo}.`));
  d.add(P("O restante da dependência virou formato de payload, endpoint e disponibilidade do serviço remoto, que o CBO não mede. Esse acoplamento é real e tem custo de teste: é por causa dele que existem os testes com `MockRestServiceServer`. **Não é correto ler a queda do CBO como \"a decomposição reduziu o acoplamento entre os domínios\".** O que caiu foi o acoplamento estático em código Java."),
    H1("7. Detalhe por classe"));
  for (const s of SV) {
    d.add(H2(s));
    d.tabela(["Classe", "Tipo", "Camada", "CBO"], CK(s).slice().sort((a, b) => b.cbo - a.cbo || a.classe.localeCompare(b.classe)).map((c) => [c.classe, c.tipo, NOME_CAMADA(c.pacote), String(c.cbo)]),
      [44, 16, 26, 14], `CBO por classe, ${s} (class.csv, sem classes anônimas).`, ["", "", "", "r"]);
  }
  d.add(H1("8. Comparação com o monólito"));
  d.tabela(["Item", "Monólito", "Microsserviços"], [
    ["Versão do ck", "0.7.0", "0.7.0"], ["Tipos analisados", String(MONO.cbo.semAnon[0]), String(CKALL.length)],
    destaque(["CBO médio", dec(MONO.cbo.semAnon[1]), dec(media(CKALL))]), ["Critério", "sem classes anônimas", "sem classes anônimas"]],
  [34, 33, 33], "Comparação do CBO com o mesmo critério.", ["", "r", "r"]);
  d.add(P(`Os denominadores não são comparáveis diretamente: ${CKALL.length} tipos contra ${MONO.cbo.semAnon[0]}. Uma parte dos tipos a mais são cópias (filtro JWT, \`JwtService\`, \`SecurityConfig\`, \`GlobalExceptionHandler\` e exceções em cada serviço), que entram na média com CBO baixo ou médio. A média menor reflete também essa replicação, e não só uma redução de dependências.`),
    H1("9. Como reproduzir"),
    CODE(["# projeto Maven auxiliar com com.github.mauricioaniche:ck:0.7.0 como única dependência", "mvn dependency:copy-dependencies -DoutputDirectory=libs", "",
      "java -cp \"libs/*\" com.github.mauricioaniche.ck.Runner \\", "    <servico>/src/main/java false 0 false docs/reports/ck/<servico>/", "",
      "# recalcular as médias a partir dos class.csv", "python docs/reports/extrair_metricas.py"]));
  return salvar(d, "Relatorio-Acoplamento-CBO-ck.docx", "Acoplamento | ck 0.7.0 | microsserviços");
}

(async () => {
  console.log("conferência:", { TESTES, RAMOS, PIT, semConfig, cbo: dec(media(CKALL)), tipos: CKALL.length });
  await relatorioJacoco(); await relatorioPitest(); await relatorioArchunit(); await relatorioCk();
})();
