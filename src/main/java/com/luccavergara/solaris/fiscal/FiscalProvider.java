package com.luccavergara.solaris.fiscal;

public interface FiscalProvider {

    EmitInvoiceResult emitInvoice(EmitInvoiceCommand command);

    EmitInvoiceResult emitCreditNote(EmitCreditNoteCommand command);
}
