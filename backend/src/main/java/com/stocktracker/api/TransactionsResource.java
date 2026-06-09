package com.stocktracker.api;

import com.stocktracker.dto.DashboardResponse;
import com.stocktracker.dto.TransactionImportCommitRequest;
import com.stocktracker.dto.TransactionImportPreviewResponse;
import com.stocktracker.dto.TransactionResponse;
import com.stocktracker.service.PortfolioService;
import com.stocktracker.service.TransactionExportService;
import com.stocktracker.service.TransactionImportService;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.nio.file.Files;
import java.util.List;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/transactions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class TransactionsResource {
  @Inject PortfolioService portfolioService;
  @Inject TransactionImportService transactionImportService;
  @Inject TransactionExportService transactionExportService;

  @GET
  public List<TransactionResponse> listTransactions() {
    return portfolioService.listTransactions();
  }

  @DELETE
  @Path("/{transactionId}")
  public DashboardResponse deleteTransaction(@PathParam("transactionId") Long transactionId) {
    return portfolioService.deleteTransaction(transactionId);
  }

  @POST
  @Path("/import/preview")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public TransactionImportPreviewResponse previewImport(@RestForm("file") FileUpload file)
      throws Exception {
    var text = Files.readString(file.uploadedFile());
    return transactionImportService.preview(text);
  }

  @POST
  @Path("/import/commit")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response commitImport(@Valid TransactionImportCommitRequest request) {
    portfolioService.createTransactions(request.rows(), "CSV_IMPORT");
    return Response.ok(portfolioService.getDashboard()).build();
  }

  @GET
  @Path("/export")
  @Produces("text/csv")
  public Response exportTransactions() {
    return Response.ok(transactionExportService.exportCsv())
        .header("Content-Disposition", "attachment; filename=\"stocktracker-transactions.csv\"")
        .build();
  }
}
