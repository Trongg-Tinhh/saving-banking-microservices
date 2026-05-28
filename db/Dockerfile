FROM postgres:15-alpine
# Bake init SQL into image so K8s doesn't need volume mounts for init scripts
COPY 01_init_schemas.sql /docker-entrypoint-initdb.d/01_init_schemas.sql
COPY 02_seed_data.sql    /docker-entrypoint-initdb.d/02_seed_data.sql
