package de.vzg.oai_importer.mycore.api.transfer;

import java.io.InputStream;
import java.util.Map;

public record TransferResult(InputStream inputStream, int statusCode, String statusMessage, Map<String, String> headers) {
}
