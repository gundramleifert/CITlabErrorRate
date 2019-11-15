package de.uros.citlab.errorrate.util;

import org.apache.commons.math3.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PageXmlUtils {
    public static List<Pair<String, String>> extractTextFromFilePairwise(String path1, String path2) {
        List<File> fileList1 = Arrays.asList(new File[]{new File(path1)});
        List<File> fileList2 = Arrays.asList(new File[]{new File(path2)});

        if (fileList1.size() != fileList2.size()) {
            return createWholeDocumentPair(fileList1, fileList2);
        }

        return createPageWisePairs(fileList1, fileList2);
    }

    public static String getTextFromFile(File f) {
        List<Node> unicodeNodes = getNodesFromFile(f);
        return getTextFromNodeList(unicodeNodes);
    }

    private static List<Pair<String, String>> createWholeDocumentPair(List<File> fileList1, List<File> fileList2) {
        String text1 = extractTextFromFileList(fileList1, "\n");
        String text2 = extractTextFromFileList(fileList2, "\n");
        return Arrays.asList(new Pair(text1, text2));
    }

    public static String extractTextFromFileList(List<File> files, String splitCharacter) {
        StringBuilder content = new StringBuilder();

        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            content.append(getTextFromFile(files.get(fileIndex)));

            if (fileIndex + 1 < files.size()) {
                content.append(splitCharacter);
            }
        }

        return content.toString();
    }

    private static List<Pair<String, String>> createPageWisePairs(List<File> fileList1, List<File> fileList2) {
        List<Pair<String, String>> pagePairs = new LinkedList<>();

        for (int fileIndex = 0; fileIndex < fileList1.size(); fileIndex++) {
            List<Node> nodeList1 = getNodesFromFile(fileList1.get(fileIndex));
            List<Node> nodeList2 = getNodesFromFile(fileList2.get(fileIndex));

            pagePairs.addAll(getPagePairs(nodeList1, nodeList2));
        }

        return pagePairs;
    }

    private static List<Node> getNodesFromXml(Document doc, String tagName) {
        try {
            NodeList nodeList = doc.getElementsByTagName(tagName);
            List<Node> unicodeNodes = new LinkedList<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                if (nodeList.item(i).getParentNode().getParentNode().getNodeName().equals("TextLine")) {
                    unicodeNodes.add(nodeList.item(i));
                }
            }

            return unicodeNodes;

        } catch (Exception ex) {
            System.out.println(ex);
            throw new RuntimeException("Could not load nodes from file!");
        }
    }

    private static List<Node> getNodesFromFile(File f) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(f);

            return getNodesFromXml(doc, "Unicode");

        } catch (Exception ex) {
            System.out.println(ex);
            throw new RuntimeException("Could not load nodes from file!");
        }
    }

    private static List<Pair<String, String>> getPagePairs(List<Node> nodeList1, List<Node> nodeList2) {
        if (allAreAlligned(nodeList1, nodeList2)) {
            List<Pair<String, String>> linePairs = new LinkedList<>();

            for (int nodeIndex = 0; nodeIndex < nodeList1.size(); nodeIndex++) {
                String text1 = nodeList1.get(nodeIndex).getTextContent();
                String text2 = nodeList2.get(nodeIndex).getTextContent();
                linePairs.add(new Pair(text1, text2));
            }

            return linePairs;
        } else {
            String text1 = getTextFromNodeList(nodeList1);
            String text2 = getTextFromNodeList(nodeList2);
            return Arrays.asList(new Pair(text1, text2));
        }
    }

    private static String getTextFromNodeList(List<Node> unicodeNodes) {
        StringBuilder content = new StringBuilder();
        int numNodes = unicodeNodes.size();
        for (int nodeIndex = 0; nodeIndex < numNodes; nodeIndex++) {
            content.append(unicodeNodes.get(nodeIndex).getTextContent());
            if (nodeIndex + 1 < numNodes) {
                content.append("\n");
            }
        }

        return content.toString();
    }

    private static boolean allAreAlligned(List<Node> nodeList1, List<Node> nodeList2) {
        if (nodeList1.size() != nodeList2.size())
            return false;

        for (int nodeIndex = 0; nodeIndex < nodeList1.size(); nodeIndex++) {
            if (!equalsBaseline(nodeList1.get(nodeIndex), nodeList2.get(nodeIndex)))
                return false;
        }

        return true;
    }

    private static Node getChild(Node parent, String name) {
        NodeList childNodes = parent.getChildNodes();
        Node res = null;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeName().equals(name)) {
                if (res != null) {
                    throw new RuntimeException("there are more than one child with this name");
                }
                res = child;
            }
        }
        return res;
    }

    private static boolean equalsBaseline(Node node1, Node node2) {
        if (node1 == node2) {
            return true;
        }

        String points1 = getChild(node1.getParentNode().getParentNode(), "Baseline").getAttributes().getNamedItem("points").getTextContent();
        String points2 = getChild(node2.getParentNode().getParentNode(), "Baseline").getAttributes().getNamedItem("points").getTextContent();
        return points1.equals(points2);
    }
}
