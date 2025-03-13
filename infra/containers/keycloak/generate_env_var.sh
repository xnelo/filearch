echo "export KEYCLOAK_ADMIN=\"admin\""
echo $(uuidgen) | tr -d '-' | tr 'a-z' 'A-Z' | (read myvar; echo "export KEYCLOAK_ADMIN_PASSWORD=\"$myvar\"";)
echo $(uuidgen) | tr -d '-' | tr 'a-z' 'A-Z' | (read myvar; echo "export FILEARCH_RESTAPI_SECRET=\"$myvar\"";)