FROM confluentinc/cp-server-connect:7.5.0 AS install-connectors
ENV CONNECT_PLUGIN_PATH: "/usr/share/java,/usr/share/confluent-hub-components"
RUN confluent-hub install --no-prompt confluentinc/kafka-connect-datagen:0.6.4

FROM confluentinc/cp-server-connect:7.5.0
COPY --from=install-connectors /usr/share/confluent-hub-components/ /usr/share/confluent-hub-components/