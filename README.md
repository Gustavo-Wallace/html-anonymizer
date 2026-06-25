# HTML Anonymizer

Ferramenta desktop em Java para anonimizar números de telefone e Internal Ticket Number em arquivos HTML.

## Objetivo

O programa recebe uma pasta de entrada, varre todos os arquivos `.html` e `.htm`, incluindo subpastas, e gera uma nova pasta com a mesma estrutura contendo apenas os HTMLs anonimizados.

## Regras

- Não modificar os arquivos originais.
- Processar somente arquivos HTML.
- Preservar a estrutura de pastas.
- Não copiar arquivos que não sejam HTML.
- Preservar a formatação original dos telefones, incluindo `+`, espaços e hífens.
- Manter anonimização consistente: se um telefone ou ticket aparecer mais de uma vez, deve receber sempre o mesmo valor anonimizado.
- Não gerar CSV.
- Gerar executável `.exe` ao final.