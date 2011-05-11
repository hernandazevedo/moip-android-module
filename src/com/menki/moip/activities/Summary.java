/**
 * Copyright (c) 2011, MENKI MOBILE SOLUTIONS - http://www.menkimobile.com.br
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, 
 * are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation and/or 
 *   other materials provided with the distribution.
 * * Neither the name of the MENKI MOBILE SOLUTIONS nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software without 
 *   specific prior written permission.
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 *  SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 *  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 *  TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 *  SUCH DAMAGE. 
 *  
 *  @version 0.0.1
 */

package com.menki.moip.activities;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.menki.moip.models.PaymentMgr;
import com.menki.moip.paymentmethods.PagamentoDireto;
import com.menki.moip.utils.MoIPResponse;
import com.menki.moip.utils.Config.PaymentType;

public class Summary extends Activity implements OnClickListener {
	private Button finish;
	private TextView summaryTextView;
	private ProgressDialog dialog;
	private Handler guiThread;
	private ExecutorService requestThread;
	private Runnable updateTask;
	private Runnable moipTask;
	private MoIPResponse response;
	private PagamentoDireto pagamentoDireto;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.summary);
		
		this.pagamentoDireto = getIntent().getParcelableExtra("PagamentoDireto");
		
		dialog = new ProgressDialog(this);
		dialog.setMessage(getString(R.string.waiting_server));
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		
		initThreading();
		
		finish = (Button) findViewById(R.id.finish_button);
		finish.setOnClickListener(this);
		
		summaryTextView = (TextView) findViewById(R.id.SummaryTextView);
		//summaryTextView.setText(summaryString());
	}
	
	private void initThreading() {
		guiThread = new Handler();
		requestThread = Executors.newSingleThreadExecutor();
		
		// this task will start the progress dialog, perform the request to moip server and dismiss the dialog.
		updateTask = new Runnable() {
			@Override
			public void run() {
				moipTask = new Runnable() {
					@Override
					public void run() {
						guiThread.post(new Runnable() {
							@Override
							public void run() {
								dialog.show();
							}
						});
						
						response = PaymentMgr.getInstance().performDirectPaymentTransaction();
						
						guiThread.post(new Runnable() {
							@Override
							public void run() {
								dialog.hide();
								Intent intent = new Intent( );
								intent.putExtra("response", response);
								setResult( RESULT_OK, intent);
								finish();
							}
						});
					} 
				};
				
				requestThread.submit(moipTask);
			} 
		};
	}

	private CharSequence summaryString() {
		HashMap<Integer, String> details = PaymentMgr.getInstance().getPaymentDetails();
		char separator1 = ' ';
		char separator2 = '\n';
		StringBuilder builder = new StringBuilder(separator2);
		
		//Set identification type string
		String idType = null;
		if (details.get(R.id.identification_type).equalsIgnoreCase(String.valueOf(R.id.radio_credit_card_cpf)))
			idType = getString(R.string.cpf);
		else
			idType = getString(R.string.rg);
		
		//Set payment type string
		String paymentType = null;
		if (details.get(R.id.payment_type).equals(String.valueOf(R.id.radio_cash_payment)))
			paymentType = getString(R.string.cash_payment);
		else
			paymentType = getString(R.string.installment_payment);
		
		
		//Set brands array
		String[] brands = getResources().getStringArray(R.array.brands_array);
		
		builder.
			// Value
			append(getString(R.string.value) + separator1).
			append(String.valueOf(PaymentMgr.getInstance().getValue()) + separator2).
			//Full Name
			append(getString(R.string.owner_name) + separator1).
			append(details.get(R.id.owner_name) + separator2). 
			//CPF or RG
			append(idType + ':' + separator1).
			append(details.get(R.id.identification_number) + separator2).
	      	//Brand
	      	append(getString(R.string.brand) + separator1).
	    	append(brands[Integer.parseInt(details.get(R.id.brand))] + separator2).
	    	//Credit card number
	    	append(getString(R.string.credit_card_number) + separator1).
	    	append(details.get(R.id.credit_card_number) + separator2).
	    	//Expiration date
	    	append(getString(R.string.expiration_date) + separator1).
	    	append(details.get(R.id.expiration_date_linearlayout) + separator2).
	    	//Secure code
	    	append(getString(R.string.secure_code) + separator1).
	    	append(details.get(R.id.secure_code) + separator2).
	    	//Payment type
	    	append(getString(R.string.payment_type) + separator1).
	    	append(paymentType + separator2);
		
			if (details.get(R.id.payment_type).equalsIgnoreCase(String.valueOf(R.id.radio_installment_payment)))
			{
				builder.append(getString(R.string.installments) + separator1).
		    	append( details.get(R.id.installments) + separator2);
			}
    	
    	return builder;
	}

	public void onClick(View v) {
		switch(v.getId())
		{
			case(R.id.finish_button):
				PaymentMgr mgr = PaymentMgr.getInstance( );
				PaymentType type = mgr.getType( );
				if(type == PaymentType.PAGAMENTO_DIRETO) {
					guiThread.post(updateTask);
				}
				else
					Log.e("MENKI [Payer] ", "Undefined Payment Method");
				break;
		}		
	}
	
	@Override
	protected void onDestroy() {
		// Terminate extra threads here
		requestThread.shutdownNow();
		super.onDestroy();
	}
}
