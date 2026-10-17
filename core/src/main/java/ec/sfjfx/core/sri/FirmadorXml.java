package ec.sfjfx.core.sri;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

/**
 * Firma el XML con XMLDSig enveloped usando el certificado .p12.
 * (Etapa simulada: firma XMLDSig valida; el refuerzo XAdES-BES completo queda pendiente.)
 */
public final class FirmadorXml {

    private FirmadorXml() {
    }

    public static byte[] firmar(byte[] xml, Path p12, char[] password) {
        try {
            Document doc = parse(xml);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (InputStream in = Files.newInputStream(p12)) {
                ks.load(in, password);
            }
            String alias = ks.aliases().nextElement();
            PrivateKey clave = (PrivateKey) ks.getKey(alias, password);
            X509Certificate certificado = (X509Certificate) ks.getCertificate(alias);

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            Reference referencia = fac.newReference("",
                    fac.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null, null);
            SignedInfo info = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    Collections.singletonList(referencia));
            KeyInfoFactory kif = fac.getKeyInfoFactory();
            KeyInfo keyInfo = kif.newKeyInfo(Collections.singletonList(
                    kif.newX509Data(Collections.singletonList(certificado))));

            DOMSignContext contexto = new DOMSignContext(clave, doc.getDocumentElement());
            XMLSignature firma = fac.newXMLSignature(info, keyInfo);
            firma.sign(contexto);
            return serializar(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el XML: " + e.getMessage(), e);
        }
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        return dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private static byte[] serializar(Document doc) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }
}
