package com.asharameta.barbershop.knowledgebase;

import org.springframework.core.io.Resource;

record KnowledgeBaseData(Resource resource, String resourceContentHash, String filename, BarbershopMetadata metadata) {}
