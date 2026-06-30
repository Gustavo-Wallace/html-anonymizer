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
- Em HTMLs com campos em `<div>`, inclusive estruturas aninhadas com labels e valores separados, valores como `Internal Ticket Number`, `Description`, `Subject`, `Target`, `Account Identifier`, e-mails, nomes, `Profile URL`, `Profile` e `Vanity Name` tambem sao anonimizados.
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

## Campos em div

O programa tambem suporta HTMLs de WhatsApp/Instagram/Facebook em que o nome do campo aparece em um `<div>` e o valor no `<div>` seguinte, inclusive quando o valor esta aninhado em estruturas como `div class="m"`:

```html
<div>Target</div>
<div>0000000000</div>

<div>Account Identifier</div>
<div>https://www.instagram.com/usuario_ficticio</div>

<div>Registered Email Addresses</div>
<div>usuario.ficticio@example.com (Verified)</div>

<div>Name</div>
<div>First</div>
<div>Joao Ficticio</div>

<div class="t i">Vanity Name</div>
<div class="m"><div>usuario_ficticio<div class="p"></div></div></div>

<div class="t i">Internal Ticket Number</div>
<div class="m"><div>0000001<div class="p"></div></div></div>
```

Tambem sao tratados campos como `Email`, `Emails Definition`, `Registered Email Addresses`, `Vanity Name`, `Profile URL`, `Profile`, `Last` e a sequencia `Middle Name` / `Full Name`. Os labels sao preservados e apenas os valores correspondentes sao substituidos por dados ficticios.

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
C:/saida/teste_anonimizado.html
```

Exemplo com pasta:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/dados/clientes" "C:/dados"
```

Resultado:

```text
C:/dados/clientes_anonimizado/a_anonimizado.html
C:/dados/clientes_anonimizado/empresa1/b_anonimizado.html
C:/dados/clientes_anonimizado/empresa2/subpasta/c_anonimizado.html
```

Quando a entrada for uma pasta, a pasta raiz gerada recebe o sufixo `_anonimizado`. As subpastas internas mantem os nomes originais, e cada arquivo HTML gerado tambem recebe `_anonimizado` antes da extensao. Se o arquivo ja terminar com `_anonimizado`, o sufixo nao e duplicado.

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
