FROM quay.io/keycloak/keycloak:20.0.3

USER root

# Ensure cache directory exists and has proper permissions
RUN mkdir -p /var/cache/yum && \
    chmod -R 777 /var/cache/yum

# Install Kerberos libraries using microdnf
RUN  microdnf update -y && microdnf install -y krb5-workstation krb5-libs nano vim && microdnf clean all && rm -rf /var/cache/yum/*

# Copy Kerberos configuration
COPY krb5.conf /etc/krb5.conf

# Copy the keytab file
COPY keycloak.keytab /opt/keycloak/data/keycloak.keytab

# Set environment variables for Kerberos
ENV KRB5_CONFIG=/etc/krb5.conf
ENV KRB5_KTNAME=/opt/keycloak/data/keycloak.keytab

## Start Keycloak using the default entry point
#ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]
