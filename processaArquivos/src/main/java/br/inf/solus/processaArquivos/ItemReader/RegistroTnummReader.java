package br.inf.solus.processaArquivos.ItemReader;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@StepScope
public class RegistroTnummReader implements ItemReader<Node> {

    private final List<Node> blocos = new ArrayList<>();
    private int currentIndex = 0;

    @StepScope
    public RegistroTnummReader(@Value("#{jobParameters['filePath']}") String filePath,
                               @Value("#{stepExecutionContext['tipo']}") String tipo) throws Exception {
        File xmlFile = new File(filePath);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(xmlFile);

        extract(doc, tipo);
    }

    private void extract(Document doc, String tag) {
        NodeList list = doc.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++) {
            blocos.add(list.item(i));
        }
    }

    @Override
    public Node read() {
        return currentIndex < blocos.size() ? blocos.get(currentIndex++) : null;
    }

}
