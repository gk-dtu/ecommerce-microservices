#!/bin/bash
# Runs automatically on first PostgreSQL container start
# Creates one database per microservice (Database per Service pattern)

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE userdb;
    CREATE DATABASE productdb;
    CREATE DATABASE orderdb;
    CREATE DATABASE authdb;

    GRANT ALL PRIVILEGES ON DATABASE userdb TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE productdb TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE orderdb TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE authdb TO $POSTGRES_USER;
EOSQL

echo "Databases userdb, productdb, orderdb, authdb created successfully"
