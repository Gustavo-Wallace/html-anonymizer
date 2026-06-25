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

Execute o arquivo `.jar` informando a pasta de entrada e a pasta de saida:

```powershell
java -jar target/html-anonymizer-1.0.0.jar "C:/entrada" "C:/saida"
```

O programa processa recursivamente arquivos `.html` e `.htm`, preserva a estrutura de pastas na saida e nao altera a pasta original.

## Uso com interface grafica

Para abrir a interface Swing simples, execute o `.jar` sem argumentos:

```powershell
java -jar target/html-anonymizer-1.0.0.jar
```

Na janela, selecione a pasta de entrada, selecione a pasta de saida e clique em `Iniciar anonimizacao`.
