package org.cygx1.escribime;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import android.app.Activity;
import android.net.http.AndroidHttpClient;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class Escribime extends Activity {
    final AndroidHttpClient http = AndroidHttpClient.newInstance("CygX1 browser");
    
    String userid = "jcerruti";
    String password = "xxx";
    String label = "other";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        System.setProperty("org.xml.sax.driver","org.xmlpull.v1.sax2.Driver");
        
        final Button tryButton = (Button) findViewById(R.id.Button01);
        tryButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				new Thread(new Runnable() {
					public void run() {
						String text = "default";
						try {
						    HttpUriRequest request = new HttpGet("https://mail.google.com/mail/feed/atom/"+ label);
					        request.addHeader("Authorization", "Basic " + new String(Base64.encode((userid + ":" + password).getBytes(),Base64.NO_WRAP)));
							HttpResponse response = http.execute(request);
							InputStream is = response.getEntity().getContent();
							
							DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
							DocumentBuilder db = dbf.newDocumentBuilder();
							Document doc = db.parse(new InputSource(is));
							doc.getDocumentElement().normalize();
							NodeList nodeList = doc.getElementsByTagName("fullcount");
							Node node = nodeList.item(0);
							int unread = Integer.parseInt(node.getTextContent());
							text = "There are " + unread + " unread messages";
							//text = slurp(is);
						} catch (IOException e) {
							text = e.toString();
						} catch (ParserConfigurationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (SAXException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						final String theText = text;
						Escribime.this.runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(Escribime.this, theText, Toast.LENGTH_SHORT).show();
							}
						});
					}
				}).start();
			}
		});
    }
    
    public static String slurp(InputStream in) throws IOException {
        StringBuffer out = new StringBuffer();
        byte[] b = new byte[4096];
        for (int n; (n = in.read(b)) != -1;) {
            out.append(new String(b, 0, n));
        }
        return out.toString();
    }
}