# HTML Anonymizer

Ferramenta desktop em Java para anonimizar numeros de telefone e Internal Ticket Number em arquivos HTML.

## Objetivo

O programa recebe uma pasta de entrada, varre todos os arquivos `.html` e `.htm`, incluindo subpastas, e gera uma nova pasta com a mesma estrutura contendo apenas os HTMLs anonimizados.

## Regras

- Nao modificar os arquivos originais.
- Processar somente arquivos HTML.
- Preservar a estrutura de pastas.
- Nao copiar arquivos que nao sejam HTML.
- Preservar a formatacao original dos telefones, incluindo `+`, espacos e hifens.
- Manter anonimizacao consistente: se um telefone ou ticket aparecer mais de uma vez, deve receber sempre o mesmo valor anonimizado.
- Nao gerar CSV.
- Gerar executavel `.exe` ao final.

## Uso via terminal

Compile o projeto com Maven:

```powershell
mvn package
```

Execute o arquivo `.jar` informando um arquivo HTML ou uma pasta de entrada, e a pasta de saida:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/entrada" "C:/saida"
```

Exemplo com arquivo individual:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/entrada/teste.html" "C:/saida"
```

Exemplo com pasta:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/clientes/empresa_a" "C:/anonimizados"
```

Quando a entrada for um arquivo `.html` ou `.htm`, o arquivo anonimizado sera gravado diretamente na pasta de saida com o mesmo nome. Quando a entrada for uma pasta, o programa cria dentro da saida uma pasta raiz com o nome da entrada acrescido de `_anonimizado`, preserva a estrutura de subpastas e nao altera a pasta original.

## Uso com interface grafica

Para abrir a interface Swing simples, execute o `.jar` sem argumentos:

```powershell
java -jar target/html-anonymizer-1.0.0.jar
```

Na janela, selecione um arquivo HTML ou uma pasta de entrada e a pasta de saida pelos botoes `Selecionar...`, ou arraste e solte um arquivo `.html`/`.htm` ou uma pasta diretamente no campo de entrada. Depois clique em `Iniciar anonimizacao`.

A pasta original nao e modificada. A pasta de saida e criada automaticamente quando necessario. Para entradas em pasta, a saida mantem a estrutura de subpastas dentro da pasta raiz `_anonimizado`, mas contendo somente os arquivos HTML processados. Arquivos que nao sao `.html` ou `.htm` nao sao copiados.
