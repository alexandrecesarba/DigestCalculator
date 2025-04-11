// Alexandre (2010292) e Enrico (2110927)

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class DigestCalculator {

    // Classe auxiliar para armazenar o resultado do digest calculado para cada arquivo.
    static class FileResult {
        String fileName;
        String digest;
        File file;

        public FileResult(String fileName, String digest, File file) {
            this.fileName = fileName;
            this.digest = digest;
            this.file = file;
        }
    }

    public static void main(String[] args) {
        // Verificação dos parâmetros da linha de comando.
        if (args.length < 3) {
            System.out.println("Uso: java DigestCalculator <Tipo_Digest> <Caminho_da_Pasta_dos_Arquivos> <Caminho_ArqListaDigest>");
            return;
        }

        String digestType = args[0].toUpperCase();
        if (!(digestType.equals("MD5") || digestType.equals("SHA1") ||
              digestType.equals("SHA256") || digestType.equals("SHA512"))) {
            System.out.println("Tipo de digest inválido. Utilize MD5, SHA1, SHA256 ou SHA512.");
            return;
        }

        String folderPath = args[1];
        String xmlFilePath = args[2];

        // Verifica a existência e validade da pasta.
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Pasta inválida: " + folderPath);
            return;
        }

        // Inicializa o MessageDigest para o tipo de digest especificado.
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(digestType);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Algoritmo não disponível: " + digestType);
            return;
        }

        // Processa cada arquivo presente na pasta, calculando o digest.
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("A pasta está vazia: " + folderPath);
            return;
        }

        List<FileResult> fileResults = new ArrayList<FileResult>();
        for (File file : files) {
            if (file.isFile()) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    md.reset();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    // Atualiza o digest de forma incremental, lendo blocos dos arquivos (1024 blocos por vez).
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        md.update(buffer, 0, bytesRead);
                    }
                    byte[] digestBytes = md.digest();
                    // Converte os bytes calculados para hexadecimal.
                    StringBuilder sb = new StringBuilder();
                    for (byte b : digestBytes) {
                        sb.append(String.format("%02x", b));
                    }
                    String digestHex = sb.toString();
                    fileResults.add(new FileResult(file.getName(), digestHex, file));
                } catch (IOException e) {
                    System.out.println("Erro ao ler o arquivo " + file.getName() + ": " + e.getMessage());
                }
            }
        }

        // Mapeia os digests calculados: digest -> lista de nomes de arquivos.
        Map<String, List<String>> computedDigestMap = new HashMap<String, List<String>>();
        for (FileResult result : fileResults) {
            List<String> list = computedDigestMap.get(result.digest);
            if (list == null) {
                list = new ArrayList<String>();
                computedDigestMap.put(result.digest, list);
            }
            list.add(result.fileName);
        }

        // Faz o parse do arquivo XML contendo a lista de digests.
        Document xmlDoc = null;
        Element catalogElement = null;
        // Mapa que relaciona nome do arquivo (conforme o XML) a uma tabela de (digestType, digestHex).
        Map<String, Map<String, String>> xmlEntries = new HashMap<String, Map<String, String>>();
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            if (xmlFile.exists()) {
                xmlDoc = dBuilder.parse(xmlFile);
                xmlDoc.getDocumentElement().normalize();
                catalogElement = (Element) xmlDoc.getDocumentElement();
            } else {
                // Se o arquivo XML não existe, cria um novo documento.
                xmlDoc = dBuilder.newDocument();
                catalogElement = xmlDoc.createElement("CATALOG");
                xmlDoc.appendChild(catalogElement);
            }

            // Percorre todos os elementos FILE_ENTRY do XML.
            NodeList fileEntryList = catalogElement.getElementsByTagName("FILE_ENTRY");
            for (int i = 0; i < fileEntryList.getLength(); i++) {
                Node node = fileEntryList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element fileEntry = (Element) node;
                    String fileName = "";
                    NodeList fileNameList = fileEntry.getElementsByTagName("FILE_NAME");
                    if (fileNameList.getLength() > 0) {
                        fileName = fileNameList.item(0).getTextContent().trim();
                    }
                    Map<String, String> digestMap = new HashMap<String, String>();
                    NodeList digestEntries = fileEntry.getElementsByTagName("DIGEST_ENTRY");
                    for (int j = 0; j < digestEntries.getLength(); j++) {
                        Node dNode = digestEntries.item(j);
                        if (dNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element digestEntry = (Element) dNode;
                            String type = "";
                            String hex = "";
                            NodeList typeList = digestEntry.getElementsByTagName("DIGEST_TYPE");
                            if (typeList.getLength() > 0) {
                                type = typeList.item(0).getTextContent().trim();
                            }
                            NodeList hexList = digestEntry.getElementsByTagName("DIGEST_HEX");
                            if (hexList.getLength() > 0) {
                                hex = hexList.item(0).getTextContent().trim();
                            }
                            digestMap.put(type.toUpperCase(), hex);
                        }
                    }
                    xmlEntries.put(fileName, digestMap);
                }
            }
        } catch (Exception e) {
            System.out.println("Erro ao processar o arquivo XML: " + e.getMessage());
            return;
        }

        // Processa os resultados calculados, determinando o STATUS de cada arquivo.
        // Status: OK, NOT OK, NOT FOUND ou COLISION.
        List<FileResult> notFoundList = new ArrayList<FileResult>(); // para atualizações no XML
        for (FileResult result : fileResults) {
            String status = "";
            boolean collision = false;

            // Verifica colisões: se o mesmo digest aparece para mais de um arquivo na pasta…
            List<String> sameDigestFiles = computedDigestMap.get(result.digest);
            if (sameDigestFiles != null && sameDigestFiles.size() > 1) {
                collision = true;
            } else {
                // …ou se o mesmo digest é encontrado no XML para um arquivo de nome diferente.
                for (Map.Entry<String, Map<String, String>> entry : xmlEntries.entrySet()) {
                    String xmlFileName = entry.getKey();
                    if (!xmlFileName.equals(result.fileName)) {
                        Map<String, String> typeMap = entry.getValue();
                        String xmlDigest = typeMap.get(digestType);
                        if (xmlDigest != null && xmlDigest.equalsIgnoreCase(result.digest)) {
                            collision = true;
                            break;
                        }
                    }
                }
            }

            if (collision) {
                status = "COLISION";
            } else {
                // Sem colisão: verifica se já existe um registro no XML para o arquivo.
                if (xmlEntries.containsKey(result.fileName)) {
                    Map<String, String> typeMap = xmlEntries.get(result.fileName);
                    if (typeMap.containsKey(digestType)) {
                        String xmlDigest = typeMap.get(digestType);
                        if (xmlDigest.equalsIgnoreCase(result.digest)) {
                            status = "OK";
                        } else {
                            status = "NOT OK";
                        }
                    } else {
                        status = "NOT FOUND";
                        notFoundList.add(result);
                    }
                } else {
                    status = "NOT FOUND";
                    notFoundList.add(result);
                }
            }

            // Imprime o resultado no formato especificado:
            // Nome_Arq <SP> Tipo_Digest <SP> Digest_Hex <SP> (STATUS)
            System.out.println(result.fileName + " " + digestType + " " + result.digest + " (" + status + ")");
        }

        // Atualiza o arquivo XML com os arquivos cujo status foi NOT FOUND.
        // Para cada um, adiciona o digest calculado:
        // - Se existir FILE_ENTRY para o arquivo, acrescenta um novo DIGEST_ENTRY.
        // - Caso contrário, cria um novo FILE_ENTRY.
        for (FileResult result : notFoundList) {
            Element fileEntryElement = null;
            if (xmlEntries.containsKey(result.fileName)) {
                // Procura a entrada existente no XML.
                NodeList fileEntryNodes = catalogElement.getElementsByTagName("FILE_ENTRY");
                for (int i = 0; i < fileEntryNodes.getLength(); i++) {
                    Node node = fileEntryNodes.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element elem = (Element) node;
                        NodeList fileNameList = elem.getElementsByTagName("FILE_NAME");
                        if (fileNameList.getLength() > 0) {
                            String existingFileName = fileNameList.item(0).getTextContent().trim();
                            if (existingFileName.equals(result.fileName)) {
                                fileEntryElement = elem;
                                break;
                            }
                        }
                    }
                }
            } else {
                // Se não existir, cria um novo FILE_ENTRY.
                fileEntryElement = xmlDoc.createElement("FILE_ENTRY");
                Element fileNameElement = xmlDoc.createElement("FILE_NAME");
                fileNameElement.appendChild(xmlDoc.createTextNode(result.fileName));
                fileEntryElement.appendChild(fileNameElement);
                catalogElement.appendChild(fileEntryElement);
                // Atualiza o mapa local.
                xmlEntries.put(result.fileName, new HashMap<String, String>());
            }

            // Cria um novo DIGEST_ENTRY para o tipo de digest utilizado.
            Element digestEntryElement = xmlDoc.createElement("DIGEST_ENTRY");
            Element digestTypeElement = xmlDoc.createElement("DIGEST_TYPE");
            digestTypeElement.appendChild(xmlDoc.createTextNode(digestType));
            Element digestHexElement = xmlDoc.createElement("DIGEST_HEX");
            digestHexElement.appendChild(xmlDoc.createTextNode(result.digest));
            digestEntryElement.appendChild(digestTypeElement);
            digestEntryElement.appendChild(digestHexElement);
            fileEntryElement.appendChild(digestEntryElement);
        }

        // Salva o documento XML atualizado no mesmo arquivo.
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(xmlDoc);
            StreamResult streamResult = new StreamResult(new File(xmlFilePath));
            transformer.transform(source, streamResult);
        } catch (TransformerException e) {
            System.out.println("Erro ao salvar o arquivo XML: " + e.getMessage());
        }
    }
}
