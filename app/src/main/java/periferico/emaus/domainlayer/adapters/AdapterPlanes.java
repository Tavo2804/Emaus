package periferico.emaus.domainlayer.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import periferico.emaus.R;
import periferico.emaus.domainlayer.WS;
import periferico.emaus.domainlayer.firebase_objects.ConfiguracionPlanes_Firebase;
import periferico.emaus.domainlayer.firebase_objects.Object_Firebase;
import periferico.emaus.domainlayer.firebase_objects.Plan_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.FormasPago_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.FrecuenciasPago_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.MatrizPlanes_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.Financiamientos_Firebase;

/**
 * Created by maubocanegra on 14/12/17.
 */

public class AdapterPlanes extends RecyclerView.Adapter<AdapterPlanes.ViewHolder> implements WS.FirebaseObjectRetrieved{

    private ArrayList<Object_Firebase> mDataset;

    private MatrizPlanes_Firebase configPlan;
    private Financiamientos_Firebase mensualidadesFirebase;
    private FormasPago_Firebase formasPagoFirebase;
    private Financiamientos_Firebase financiamientoFirebase;
    private FrecuenciasPago_Firebase frecuenciasPagoFirebase;



    Context c;

    public AdapterPlanes(ArrayList<Object_Firebase> myDataset, Context context){
        mDataset = myDataset;
        c = context;

        WS.readConfiguracionPlanes(AdapterPlanes.this);
    }

    // ----------------------------------------------------------- //
    // ---------------- VIEWHOLDER IMPLEMENTATION ---------------- //
    //------------------------------------------------------------ //

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_planes, parent, false);

        /*
        planes = c.getResources().getStringArray(R.array.nuevoplan_array_planes);
        ataudes = new String[][]{
                c.getResources().getStringArray(R.array.nuevoplan_array_planbasico),
                c.getResources().getStringArray(R.array.nuevoplan_array_planLujo),
                c.getResources().getStringArray(R.array.nuevoplan_array_planmadera)
        };
        servicios = c.getResources().getStringArray(R.array.nuevoplan_array_servicio);
        financiamientos = c.getResources().getStringArray(R.array.nuevoplan_array_financiamiento);
        pagos = c.getResources().getStringArray(R.array.nuevoplan_array_pago);
        formaPago = c.getResources().getStringArray(R.array.nuevoplan_array_formapago);
        */

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Plan_Firebase planFirebase = ((Plan_Firebase)mDataset.get(position));
        holder.textviewContrato.setText(planFirebase.getStID());

        try {
            holder.textviewPlan.setText(configPlan.getPlanByPlanID(planFirebase.getPlanID()).getNombre());
            holder.textviewAtaud.setText(configPlan.getPlanByPlanID(planFirebase.getPlanID()).getAtaudByID(planFirebase.getAtaudID()).getNombre());
            holder.textviewServicio.setText(configPlan.getPlanByPlanID(planFirebase.getPlanID()).getAtaudByID(planFirebase.getAtaudID()).getServicioByID(planFirebase.getServicioID()).getNombre());
            holder.textviewFinanciamiento.setText(financiamientoFirebase.getFinanciamientoByID(planFirebase.getFinanciamientoID()).getNombre());
            holder.textviewFrecuencia.setText(frecuenciasPagoFirebase.getFrecuenciaByID(planFirebase.getFrecuenciaPagoID()).getNombre());
            holder.textviewForma.setText(formasPagoFirebase.getFormaPagoByID(planFirebase.getFormaPagoID()).getNombre());


            String montoFormateado = NumberFormat.getNumberInstance(Locale.US).format(planFirebase.getTotalAPagar());
            holder.textviewTotal.setText(String.format( c.getString(R.string.nuevoplan_formatted_monto),montoFormateado));
        }catch(Exception e){}

    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    // -------------------------------------------------- //
    // ---------------- VIEWHOLDER CLASS ---------------- //
    //--------------------------------------------------- //

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView textviewContrato;
        private TextView textviewPlan;
        private TextView textviewAtaud;
        private TextView textviewTotal;
        private TextView textviewServicio;
        private TextView textviewFinanciamiento;
        private TextView textviewFrecuencia;
        private TextView textviewForma;

        ViewHolder(View view){
            super(view);
            textviewContrato = view.findViewById(R.id.itemplan_textview_contrato);
            textviewPlan = view.findViewById(R.id.itemplan_textview_plan);
            textviewAtaud = view.findViewById(R.id.itemplan_textview_ataud);
            textviewTotal = view.findViewById(R.id.itemplan_textview_total);
            textviewServicio = view.findViewById(R.id.itemplan_textview_servicio);
            textviewFinanciamiento = view.findViewById(R.id.itemplan_textview_financiamiento);
            textviewFrecuencia = view.findViewById(R.id.itemplan_textview_frecpago);
            textviewForma = view.findViewById(R.id.itemplan_textview_formapago);
        }
    }

    // ----------------------------------------------------------- //
    // ---------------- FIREBASE OBJECT RETRIEVED ---------------- //
    //------------------------------------------------------------ //


    @Override
    public void firebaseObjectRetrieved(Object_Firebase objectFirebase) {
        ConfiguracionPlanes_Firebase configuracionPlanesFirebase = (ConfiguracionPlanes_Firebase)objectFirebase;

        configPlan = configuracionPlanesFirebase.getPlanes();
        mensualidadesFirebase = configuracionPlanesFirebase.getListamensualidades();
        formasPagoFirebase = configuracionPlanesFirebase.getListaformaspago();

        financiamientoFirebase = configuracionPlanesFirebase.getListamensualidades();
        frecuenciasPagoFirebase = configuracionPlanesFirebase.getListafrecuenciaspago();
    }
}
