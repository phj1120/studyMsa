rootProject.name = "open-api-platform"

include(
    "idp",
    "open-api-gateway:public",
    "tenant-admin",
    "openapi-test",
    "business:business-tenant",
    "business:business-auth",
    "support:common",
    "support:web-api",
    "support:logger",
    "support:jwt",
    "support:security",
    "storages:db-core",
    "storages:redis-core",
)
