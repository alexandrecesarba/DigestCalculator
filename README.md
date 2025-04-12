# Trabalho 2 - DigestCalculator

### Alexandre (2010292) e Enrico (2110927)

Construir um programa Java que

- use a JCA
- não use interface gráfica
- seja executado em uma linha de comando com argumentos, da seguinte forma:

```bash
DigestCalculator <SP> Tipo_Digest <SP> Caminho_da_Pasta_dos_Arquivos <SP> Caminho_ArqListaDigest
```

onde,

- `Tipo_Digest`: Tipo do digest a ser calculado (MD5/SHA1/SHA256/SHA512)
- `Caminho_da_Pasta_dos_Arquivos`: Informa a localização (caminho) da pasta que contém os arquivos que devem ser processados
- `Caminho_ArqListaDigest`: Informa a localização do arquivo que contém uma lista de digests de arquivos conhecidos
- `<SP>`: Caractere espaço em branco

O arquivo com a lista de digest utiliza o formato XML e é formado _por zero ou mais linhas_ formatadas da seguinte
maneira:

```xml
<CATALOG>
    <FILE_ENTRY>
        <FILE_NAME>Nome_Arq</FILE_NAME>
        <DIGEST_ENTRY>
            <DIGEST_TYPE>Tipo_Digest</DIGEST_TYPE>
            <DIGEST_HEX>Digest_Hex</DIGEST_HEX>
        </DIGEST_ENTRY>
    </FILE_ENTRY>
</CATALOG>
```

onde,

- `Nome_Arq`: Nome de um arquivo qualquer, _sem informar o caminho_
- `TipoDigest`: Indica o digest em seguida (MD5/SHA1/SHA256/SHA512)
- `Digest_Hex`: Digest em hexadecimal referente ao tipo de digest especificado anteriormente

## Observações

- Um `<CATALOG>` é formado por zero ou mais `<FILE_ENTRY>`
- Um `<FILE_ENTRY>` possui um `<FILE_NAME>` e um ou mais `<DIGEST_ENTRY>`
- Um `<DIGEST_ENTRY>` possui um `<DIGEST_TYPE>` e um `<DIGEST_HEX>`

## Exemplo

```xml
<CATALOG>
    <FILE_ENTRY>
        <FILE_NAME>Arquivo1.dat</FILE_NAME>
        <DIGEST_ENTRY>
            <DIGEST_TYPE>SHA1</DIGEST_TYPE>
            <DIGEST_HEX>8d901bb3a2840ac030f7dbdd7cb823808858cb2f</DIGEST_HEX>
        </DIGEST_ENTRY>
        <DIGEST_ENTRY>
            <DIGEST_TYPE>MD5</DIGEST_TYPE>
            <DIGEST_HEX>42b83991bd1b47b373074111c34fb428</DIGEST_HEX>
        </DIGEST_ENTRY>
    </FILE_ENTRY>
    <FILE_ENTRY>
        <FILE_NAME>Arquivo2.dat</FILE_NAME>
        <DIGEST_ENTRY>
            <DIGEST_TYPE>SHA256</DIGEST_TYPE>
            <DIGEST_HEX>c8db093d264aa744d178470ad97aa64e67e84ab96e3b3310fb6f0eda429e6622</DIGEST_HEX>
        </DIGEST_ENTRY>
    </FILE_ENTRY>
</CATALOG>
```

Neste exemplo, o `Arquivo1.dat` tem dois digests na sua lista (`SHA1` e `MD5`) enquanto o 
`Arquivo2.dat` tem apenas um digest na sua lista. Porém, ambos poderiam ter até 4 digests nas
suas respectivas listas, um de cada tipo, em qualquer ordem

O programa deve executar o seguinte procedimento:

1. Calcular o digest solicitado do conteúdo de todos os arquivos presentes na pasta fornecida.
2. Comparar os digests calculados com os respectivos digests registrados para cada arquivo
no arquivo `ArqListaDigest`, se existirem, _e com os digests dos arquivos existentes na pasta_
3. Imprimir, na saída padrão, uma _lista com o formato especificado abaixo_ 

```text
Nome_Arq1<SP>Tipo_Digest<SP>Digest_Hex_Arq1<SP>(STATUS)
Nome_Arq2<SP>Tipo_Digest<SP>Digest_Hex_Arq2<SP>(STATUS)
. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
Nome_ArqN<SP>Tipo_Digest<SP>Digest_Hex_ArqN<SP>(STATUS)
```

onde,

- `<SP>`: Caracter espaço em branco
- `Nome_Arq1` .. `Nome_ArqN`: Correspondem aos nomes dos arquivos encontrados na pasta fornecida
para o cálculo dos digests (_sem informação do caminho da pasta_)
- `Tipo_Digest`: Tipo do digest calculado (MD5/SHA1/SHA256/SHA512)
- `Digest_Hex_ArqN`: Digest formatado em hexadecimal calculado para o arquivo N.
- `STATUS`: Corresponde a um dos status definidos abaixo:
  - `OK`: Digest calculado é igual ao digest fornecido no arquivo `ArqListaDigest` e não colidem com o digest 
de outro arquivo na pasta.
  - `NOT_OK`: Digest calculado não é igual ao digest fornecido no arquivo `ArqListaDigest` e não colidem com o digest
de outro arquivo na pasta.
  - `NOT_FOUND`: Digest não foi encontrado no arquivo `ArqListaDigest` e não colide com o digest de outro arquivo na pasta
  - `COLISION`: Digest calculado colide com o digest de outro arquivo de nome diferente encontrado no arquivo `ArqListaDigest`
ou com o digest de um dos arquivos presentes na pasta

Os digests calculados para os arquivos `NOT FOUND` devem ser acrescentados no registro de um 
nome de arquivo existente ou com uma nova entrada de um novo arquivo no nofinal do arquivo de lista
de digests, mantendo seu formato padrão. _Os digests calculados para os arquivos com status `COLISION`
não devem ser acrescentados no arquivo de lista de digests_.

## Observações finais

1. O nome do programa executável deve ser DigestCalculator
2. Estude os métodos `update` da classe `MessageDigest` e selecione o método adequado para este trabalho.
3. Os digests devem ser calculados para o _conteúdo dos arquivos_ presentes na pasta fornecida na linha de comando
e não para o _nome dos arquivos_ que estão na pasta
4. Se os argumentos na linha de comando forem omitidos ou insuficientes para a execução do programa,
deve-se imprimir uma mensagem com a orientação de execução e, em seguida, o programa deve ser encerrado.