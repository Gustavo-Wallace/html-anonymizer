# HTML Anonymizer

Ferramenta desktop em Java para anonimizar telefones e valores de `Internal Ticket Number` em arquivos HTML.

## Objetivo

O programa processa um arquivo HTML individual ou uma pasta inteira com arquivos `.html` e `.htm`, gerando uma saida anonimizada sem modificar os arquivos originais.

## Regras de anonimizacao

- Telefones sao substituidos mantendo, sempre que possivel, `+`, espacos e hifens nas mesmas posicoes.
- O mesmo telefone recebe sempre o mesmo valor anonimizado durante uma execucao.
- Valores de `Internal Ticket Number` sao anonimizados apenas no numero apos esse texto.
- O mesmo ticket recebe sempre o mesmo valor anonimizado durante uma execucao.
- Em HTMLs com campos em tabela, os campos `Internal Ticket Number`, `Description` e `Subject` tambem sao anonimizados quando aparecem em linhas `<tr>` com o nome do campo em `<th>` e o valor em `<td>`.
- Arquivos originais nunca sao modificados.
- Arquivos que nao sejam `.html` ou `.htm` nao sao copiados.
- O programa nao gera CSV.

## Campos em tabela

O programa tambem trata campos comuns em HTMLs com estrutura de tabela:

```html
<tr><th>Internal Ticket Number</th><td>0000001<br /></td></tr>
<tr><th>Description</th><td>Texto sensivel de exemplo<br /></td></tr>
<tr><th>Subject</th><td>Grupo de exemplo<br /></td></tr>
```

Nesses casos, apenas o conteudo do `<td>` correspondente e anonimizado. A estrutura HTML, incluindo `<tr>`, `<th>`, `<td>` e tags como `<br />`, e preservada.

## Uso pela interface grafica

Compile o projeto e execute o `.jar` sem argumentos:

```powershell
mvn package
java -jar target/html-anonymizer-1.0.0.jar
```

Na janela, selecione ou arraste:

- um arquivo `.html`/`.htm` individual; ou
- uma pasta de entrada.

Depois selecione a pasta de saida e clique em `Iniciar anonimizacao`.

## Uso via terminal

Execute o `.jar` informando a entrada e a pasta de saida:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/entrada" "C:/saida"
```

Exemplo com arquivo individual:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/entrada/teste.html" "C:/saida"
```

Resultado:

```text
C:/saida/teste.html
```

Exemplo com pasta:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/dados/clientes" "C:/dados"
```

Resultado:

```text
C:/dados/clientes_anonimizado/a.html
C:/dados/clientes_anonimizado/empresa1/b.html
C:/dados/clientes_anonimizado/empresa2/subpasta/c.html
```

Quando a entrada for uma pasta, apenas a pasta raiz gerada recebe o sufixo `_anonimizado`. As subpastas internas mantem os nomes originais.

## Build do JAR

Use Maven diretamente:

```powershell
mvn clean package
```

Ou use o script:

```bat
scripts\build-jar.bat
```

O arquivo sera gerado em:

```text
target/html-anonymizer-1.0.0.jar
```

## Build do EXE no Windows

Para gerar o instalador `.exe`, use um JDK que inclua `jpackage` e garanta que `mvn` e `jpackage` estejam no `PATH`.

Execute:

```bat
scripts\build-exe.bat
```

O script:

- roda `mvn clean package`;
- valida se `jpackage` esta disponivel;
- limpa saidas temporarias anteriores;
- gera o instalador em `dist/`;
- solicita a pasta de instalacao durante a instalacao;
- solicita instalacao por usuario;
- tenta criar automaticamente atalho na Area de Trabalho;
- cria entrada no Menu Iniciar no grupo `HTML Anonymizer`.

O arquivo `.exe` gerado em `dist/` e um instalador do Windows. Depois de instalar, abra o programa pelo atalho da Area de Trabalho ou pelo Menu Iniciar. O instalador nao abre o programa automaticamente ao finalizar.

Se o instalador ficar aberto no Gerenciador de Tarefas como `Installer of HTML Anonymizer` mas nenhuma janela aparecer, finalize esses processos e execute o instalador novamente. Se uma versao anterior ja estiver instalada, desinstale-a antes de testar um novo instalador. O Windows Installer pode reaproveitar o estado da instalacao anterior e nao recriar atalhos ja omitidos/removidos.

Os artefatos de build (`target/`, `dist/`, `.jar`, `.exe`, `.msi`) ficam ignorados pelo Git.
