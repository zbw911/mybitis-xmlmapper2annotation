package com.zhangbaowei.tools.mybaitistool;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author zhangbaowei
 * Create  on 2018/12/21 16:03.
 */
public class Main {
    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {

        Main main = new Main();

        //下面两个路径从参数传吧，这仅是 用于测试的Demo
        List<XmlStruct> stringXmlStructMap = main.toConvert("E:\\TagMapper.xml");

        String javaFile = "E:\\TagMapper.java";


        LinkedList<String> list = main.readFile(javaFile);

        String s = main.GenNewJavaFile(list, stringXmlStructMap);

        list.forEach(x -> System.out.println(x));

    }

    public static String toUpperFristChar(String string) {
        char[] charArray = string.toCharArray();
        charArray[0] -= 32;
        return String.valueOf(charArray);
    }

    public String GenNewJavaFile(LinkedList<String> fileline, List<XmlStruct> stringXmlStructMap) {


        for (int i = 0; i < fileline.size(); i++) {
            if (fileline.get(i).indexOf("import ") >= 0) {
                fileline.add(i, "import org.apache.ibatis.annotations.*;");
                break;
            }
        }

        for (XmlStruct xmlStruct : stringXmlStructMap) {
            if (xmlStruct.getType().equals("select") || xmlStruct.getType().equals("update") ||
                    xmlStruct.getType().equals("delete") ||
                    xmlStruct.getType().equals("insert")) {
                String id = xmlStruct.getId();
                for (int i = 0; i < fileline.size(); i++) {
                    if (fileline.get(i).indexOf(" " + id + "(") > 0) {
                        fileline.add(i, "@" + toUpperFristChar(xmlStruct.getType()) + "(" + wapperToScript(xmlStruct.getConent()) + ")");
//
                        if (xmlStruct.getResultMap() != null && xmlStruct.getResultMap().length() > 0) {
                            Optional<XmlStruct> resultMap = stringXmlStructMap.stream().filter(x -> x.getType().equals("resultMap") && x.getId().equals(xmlStruct.getResultMap())).findFirst();

                            XmlStruct xmlStruct1 = resultMap.get();
                            String resultStr = "    @Results(";

                            resultStr += "id = \"" + xmlStruct1.getId() + "\",\n";
                            resultStr += "            value = {\r\n";

                            resultStr += xmlStruct1.getConent();
                            resultStr += "})";

                            fileline.add(i, resultStr);

                        }

                        break;
                    }
                }
            }


        }

        return "";
    }

    public LinkedList<String> readFile(String javafile) throws IOException {

        LinkedList<String> list = new LinkedList<>();

        FileInputStream inputStream = new FileInputStream(javafile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String str = null;
        while ((str = bufferedReader.readLine()) != null) {
//            System.out.println(str);

            list.add(str);
        }
        //close
        inputStream.close();
        bufferedReader.close();


        return list;

    }


    public List<XmlStruct> toConvert(String xmlfile) throws ParserConfigurationException, IOException, SAXException {

        List<XmlStruct> list = new LinkedList<>();

        File inputFile = new File(xmlfile);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();


        NodeList childNodes = doc.getElementsByTagName("mapper");

        int length = childNodes.getLength();

        childNodes = childNodes.item(0).getChildNodes();

        length = childNodes.getLength();

        for (int i = 0; i < childNodes.getLength(); i++) {

            Node item = childNodes.item(i);


            String nodeName = item.getNodeName();

            if (nodeName.equals("select")) {
                list.add(opQUAD(item));
            } else if (nodeName.equals("delete")) {
                list.add(opQUAD(item));
            } else if (nodeName.equals("insert")) {
                list.add(opQUAD(item));
            } else if (nodeName.equals("update")) {
                list.add(opQUAD(item));
            } else if (nodeName.equals("resultMap")) {
                list.add(opResultMap(item));
            }

//            System.out.println("************************************");
//            System.out.println(outterXML(item));
//            System.out.println("************************************");

        }


        return list;
    }


    private String wapperToScript(String source) {

        StringBuilder sb = new StringBuilder();

        sb.append("\"<script>\\n\"+\n");

        String[] split = source.split("\\n");


        for (int i = 0; i < split.length; i++) {
            String line = split[i];

            if (line == null || line.length() == 0 || line.trim().length() == 0) {
                continue;
            }
            line = line.replace("\"", "\\\"");

            sb.append("\"").append(line).append("\\n\"").append("+\n");
        }

        sb.append("\"    </script>\"");

        return sb.toString();
    }

    private XmlStruct opQUAD(Node item) {

        XmlStruct xmlStruct = new XmlStruct();

        Node id = item.getAttributes().getNamedItem("id");
        xmlStruct.setId(id.getNodeValue());
        xmlStruct.setConent(innerXml(item));
        xmlStruct.setType(item.getNodeName());


        Node resultMap = item.getAttributes().getNamedItem("resultMap");
        if (resultMap != null) {
            xmlStruct.setResultMap(resultMap.getNodeValue());
        }

        return xmlStruct;
    }

    private XmlStruct opResultMap(Node item) {

        XmlStruct xmlStruct = new XmlStruct();
        //  @Results(id = "userMap",
        Node id = item.getAttributes().getNamedItem("id");
        if (id != null) {
            xmlStruct.setId(id.getNodeValue());
        }

        StringBuilder sb = new StringBuilder();

        for (int i1 = 0; i1 < item.getChildNodes().getLength(); i1++) {
            Node item1 = item.getChildNodes().item(i1);
            String nodeName1 = item1.getNodeName();
            if (nodeName1.equals("result") || nodeName1.equals("id")) {
//  @Result(column = "id", property = "id",
                if (nodeName1.equals("id")) {
                    sb.append("//" + outterXML(item1) + "\r\n");
                }
                Node column = item1.getAttributes().getNamedItem("column");
                Node property = item1.getAttributes().getNamedItem("property");
                sb.append("@Result(column = \"" + column.getNodeValue() + "\", property = \"" +
                        property.getNodeValue() + "\"),\r\n");
            } else if (nodeName1.equals("collection")) {
                sb.append("//collection=> begin\r\n");
                sb.append("//有collection 情况下，这里需要手工改造为 @Many\n");
                sb.append("/**\r\n");
                sb.append(outterXML(item1));
                sb.append("\r\n**/\r\n");
                XmlStruct collect = opResultMap(item1);
                if (collect != null) {
                    sb.append(collect.getConent()).append("\r\n");
                }
                sb.append("//collection=> end\n");
            }
        }

        xmlStruct.setConent(sb.toString());
        xmlStruct.setType("resultMap");

        return xmlStruct;
    }


    public String innerXml(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation()
                .getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();

        lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);

        NodeList childNodes = node.getChildNodes();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < childNodes.getLength(); i++) {
            sb.append(lsSerializer.writeToString(childNodes.item(i)));
        }
        return sb.toString();
    }

    public String outterXML(Node node) {
        DOMImplementationLS lsImpl = (DOMImplementationLS) node.getOwnerDocument().getImplementation()
                .getFeature("LS", "3.0");
        LSSerializer lsSerializer = lsImpl.createLSSerializer();

        lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);

        StringBuilder sb = new StringBuilder();
        sb.append(lsSerializer.writeToString(node));
        return sb.toString();
    }
}


class XmlStruct {
    String id;
    String conent;
    String type;
    String resultMap;

    public String getResultMap() {
        return resultMap;
    }

    public void setResultMap(String resultMap) {
        this.resultMap = resultMap;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConent() {
        return conent;
    }

    public void setConent(String conent) {
        this.conent = conent;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
