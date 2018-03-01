#!/bin/sh

cd ./out/artifacts/WebCabinet/VAADIN/themes/webcabinet

java -cp '../../../WEB-INF/lib/*' com.vaadin.sass.SassCompiler styles.scss styles.css