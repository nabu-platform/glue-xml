package be.nabu.glue.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.nabu.glue.ScriptRuntime;
import be.nabu.glue.impl.methods.ScriptMethods;
import be.nabu.glue.impl.methods.TestMethods;
import be.nabu.libs.evaluator.annotations.MethodProviderClass;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.java.BeanInstance;
import be.nabu.libs.types.xml.XMLContent;
import be.nabu.utils.xml.BaseNamespaceResolver;
import be.nabu.utils.xml.XMLUtils;
import be.nabu.utils.xml.XPath;
import be.nabu.utils.xml.diff.SimpleDiff;
import be.nabu.utils.xml.diff.XMLDiff;

@MethodProviderClass(namespace = "xml")
public class XMLMethods {
	
	public static Node xml(Object object) throws IOException, SAXException, ParserConfigurationException {
		return xml(object, false);
	}
	
	@SuppressWarnings("rawtypes")
	public static String stringify(Object object) throws TransformerException, IOException {
		if (object instanceof Document) {
			return XMLUtils.toString(((Document) object).getDocumentElement(), true, true);
		}
		else {
			ComplexContent content;
			if (object instanceof ComplexContent) {
				content = (ComplexContent) object;
			}
			else {
				content = new BeanInstance(object);
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			XMLBinding binding = new XMLBinding(content.getType(), ScriptRuntime.getRuntime().getScript().getCharset());
			binding.marshal(output, content);
			return new String(output.toByteArray(), ScriptRuntime.getRuntime().getScript().getCharset());
		}
	}
	
	public static Object objectify(Object object) throws IOException, SAXException, ParserConfigurationException {
		String string = ScriptMethods.string(object);
		Document document = XMLUtils.toDocument(new ByteArrayInputStream(string.getBytes(ScriptRuntime.getRuntime().getScript().getCharset())), false);
		return new XMLContent(document.getDocumentElement());
	}
	
	public static Node xml(Object object, boolean namespaceAware) throws IOException, SAXException, ParserConfigurationException {
		if (object == null) {
			return null;
		}
		else if (object instanceof Node) {
			return (Node) object;
		}
		byte[] content = ScriptMethods.bytes(object);
		return XMLUtils.toDocument(new ByteArrayInputStream(content), namespaceAware);
	}
	
	public static Object xpath(Object xml, String path, String value) throws TransformerException, IOException, SAXException, ParserConfigurationException {
		if (xml == null) {
			return null;
		}
		boolean returnAsDocument = xml instanceof Document;
		Node document = xml(xml);
		BaseNamespaceResolver resolver = new BaseNamespaceResolver();
		resolver.setScanRecursively(true);
		NodeList nodeList = new XPath(path).setNamespaceContext(resolver).query(document).asNodeList();
		for (int i = 0; i < nodeList.getLength(); i++) {
			if (nodeList.item(i) instanceof Attr) {
				((Attr) nodeList.item(i)).setValue(value);
			}
			else {
				nodeList.item(i).setTextContent(value);
			}
		}
		return returnAsDocument ? document : XMLUtils.toString(document, true, true);
	}
	
	public static String xpath(Object xml, String path) throws TransformerException, IOException, SAXException, ParserConfigurationException {
		if (xml == null) {
			return null;
		}
		Node document = xml(xml);
		BaseNamespaceResolver resolver = new BaseNamespaceResolver();
		resolver.setScanRecursively(true);
		return new XPath(path).setNamespaceContext(resolver).query(document).asString();
	}
	
	public static Node[] nodes(Object xml, String path) throws TransformerException, IOException, SAXException, ParserConfigurationException {
		if (xml == null) {
			return null;
		}
		Node document = xml(xml);
		BaseNamespaceResolver resolver = new BaseNamespaceResolver();
		resolver.setScanRecursively(true);
		NodeList list = new XPath(path).setNamespaceContext(resolver).query(document).asNodeList();
		Node [] array = new Node[list == null ? 0 : list.getLength()];
		for (int i = 0; i < array.length; i++) {
			array[i] = list.item(i);
		}
		return array;
	}
	
	public static String xdiff(Object source, Object target) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		if (source == null && target == null) {
			return null;
		}
		else if (source == null && target != null) {
			return "i: /\n" + ">> " + XMLUtils.toString(xml(target, true)).replaceAll("\n", "\n>> ") + "\n";
		}
		else if (source != null && target == null) {
			return "d: /\n" + "<< " + XMLUtils.toString(xml(target, true)).replaceAll("\n", "\n<< ") + "\n";
		}
		XMLDiff diff = new XMLDiff(new SimpleDiff());
		Document sourceDocument = (Document) xml(source);
		Document targetDocument = (Document) xml(target);
		return diff.diff(sourceDocument, targetDocument).asTargetPatch();
	}
	
	public static boolean validateXMLEquals(String message, Object expected, Object actual) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		boolean result;
		if (expected == null) {
			result = TestMethods.validateTrue(message, actual == null);
		}
		else {
			result = TestMethods.validateTrue(message, actual != null);
		}
		if (result) {
			String xdiff = xdiff(actual, expected);
			if (xdiff != null && xdiff.isEmpty()) {
				xdiff = null;
			}
			result &= TestMethods.validateNull(message + " (xdiff)", xdiff);
		}
		return result;
	}
}
