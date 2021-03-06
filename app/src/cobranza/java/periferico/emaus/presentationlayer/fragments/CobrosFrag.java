package periferico.emaus.presentationlayer.fragments;


import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.CompositeMultiplePermissionsListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import periferico.emaus.R;
import periferico.emaus.domainlayer.WS;
import periferico.emaus.domainlayer.adapters.AdapterRuta;
import periferico.emaus.domainlayer.firebase_objects.Categorias_Firebase;
import periferico.emaus.domainlayer.firebase_objects.Cliente_Firebase;
import periferico.emaus.domainlayer.firebase_objects.ConfiguracionPlanes_Firebase;
import periferico.emaus.domainlayer.firebase_objects.Object_Firebase;
import periferico.emaus.domainlayer.firebase_objects.Plan_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.FinanciamientoOBJ_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.Financiamientos_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.FrecuenciaPago_Firebase;
import periferico.emaus.domainlayer.firebase_objects.configplan.FrecuenciasPago_Firebase;
import periferico.emaus.domainlayer.objetos.Direcciones;
import periferico.emaus.domainlayer.objetos.PuntoRutaWrapper;
import periferico.emaus.presentationlayer.activities.DetalleCobro;
import periferico.emaus.presentationlayer.activities.NuevoPlan;

/**
 * A simple {@link Fragment} subclass.
 */
public class CobrosFrag extends Fragment implements
        OnMapReadyCallback,
        WS.FirebaseArrayRetreivedListener,
        WS.FirebaseObjectRetrievedListener,
        View.OnClickListener,
        AdapterRuta.ComponentInItemRutaClickListener,
        AdapterRuta.ItemRemoverListener{

    View view;
    View viewTodosPermisos;
    Button buttonOpenSettings;
    GoogleMap googleMap;

    DexterBuilder dexterBuilder;

    // ---------------

    BottomSheetBehavior sheetBehaviorLista;
    BottomSheetBehavior sheetBehaviorItem;
    View bottomSheetLayoutItem;
    View bottomsheetLayoutLista;
    View headerBottomSheetLista;
    TextView tituloRutas;
    TextView subtituloRutas;
    TextView mostrarOcultarLista;
    ProgressBar progressLista;

    TextView itemNombre;
    TextView itemStatus;
    TextView itemLetras;
    TextView itemPagos;
    TextView itemDireccion;
    ImageView itemFachada;
    Button itemButtonPagar;
    Button itemButtonMover;
    String planIDSelected;
    String clienteIDSelected;


    //----------------

    RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    android.support.v7.widget.LinearLayoutManager mLayoutManager;

    //----------------

    //ArrayList<Ruta_Firebase> rutaActual;
    ArrayList<PuntoRutaWrapper> mDataset;
    String TAG = "CobrosFragDebug";
    PuntoRutaWrapper lastSelected;
    boolean initialMapSet;
    List<Map<String,Object>> mapaEstatusVisita;

    ConfiguracionPlanes_Firebase configuracionPlanesFirebase;
    FrecuenciasPago_Firebase frecuenciasPagoFirebase;
    Financiamientos_Firebase financiamientoFirebase;


    public static CobrosFrag newInstance(){return new CobrosFrag();}
    public CobrosFrag() {}


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setRetainInstance(true);
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_cobros, container, false);

        initObjects();
        setListeners();

        downloadConfigs();

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    // --------------------------------------------- //
    // ---------------- OWN METHODS ---------------- //
    //---------------------------------------------- //

    private void initObjects(){
        buttonOpenSettings = view.findViewById(R.id.cobros_abrir_ajustes);
        viewTodosPermisos = view.findViewById(R.id.layout_todos_permisos);

        bottomSheetLayoutItem = view.findViewById(R.id.bottomsheet_item);
        sheetBehaviorItem = BottomSheetBehavior.from(bottomSheetLayoutItem);
        sheetBehaviorItem.setState(BottomSheetBehavior.STATE_HIDDEN);
        itemNombre = bottomSheetLayoutItem.findViewById(R.id.item_ruta_nombrecliente);
        itemStatus = bottomSheetLayoutItem.findViewById(R.id.item_ruta_estadocliente);
        itemLetras = bottomSheetLayoutItem.findViewById(R.id.item_perfilcobrador_letras);
        itemPagos = bottomSheetLayoutItem.findViewById(R.id.item_perfilcobrador_pagos);
        itemDireccion = bottomSheetLayoutItem.findViewById(R.id.item_ruta_direccion);
        itemFachada = bottomSheetLayoutItem.findViewById(R.id.item_perfilcobrador_fachada);
        itemButtonPagar = bottomSheetLayoutItem.findViewById(R.id.item_perfilcobrador_pagado);
        itemButtonMover = bottomSheetLayoutItem.findViewById(R.id.item_perfilcobrador_fecha);

        bottomsheetLayoutLista = view.findViewById(R.id.bottomsheet_ruta);
        headerBottomSheetLista = view.findViewById(R.id.bottomsheet_ruta_header);
        tituloRutas = view.findViewById(R.id.bottomsheet_ruta_titulo_ruta);
        subtituloRutas = view.findViewById(R.id.bottomsheet_ruta_subtitulo_ruta);
        mostrarOcultarLista = view.findViewById(R.id.bottomsheet_ruta_label_mostrar_ocultar);

        progressLista = view.findViewById(R.id.progressBar4);

        mRecyclerView = view.findViewById(R.id.recyclerView);

        mDataset = new ArrayList<>();

        mAdapter = new AdapterRuta(mDataset, getActivity(), CobrosFrag.this, CobrosFrag.this);
        mRecyclerView.setAdapter(mAdapter);

        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
    }

    private void setListeners(){
        buttonOpenSettings.setOnClickListener(CobrosFrag.this);
        itemButtonPagar.setOnClickListener(CobrosFrag.this);
        itemButtonMover.setOnClickListener(CobrosFrag.this);
    }

    private void downloadConfigs(){
        WS.readConfiguracionPlanes(new WS.FirebaseObjectRetrievedListener() {
            @Override
            public void firebaseObjectRetrieved(Object_Firebase objectFirebase) {
                configuracionPlanesFirebase = (ConfiguracionPlanes_Firebase)objectFirebase;
                frecuenciasPagoFirebase = configuracionPlanesFirebase.getListafrecuenciaspago();
                financiamientoFirebase = configuracionPlanesFirebase.getListamensualidades();
            }
        });

        WS.readEstatusVisita(new WS.FirebaseObjectRetrievedListener() {
            @Override
            public void firebaseObjectRetrieved(final Object_Firebase objectFirebase) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mapaEstatusVisita = ((Categorias_Firebase)objectFirebase).getEstatusVisita();
                    }
                });
            }
        });
    }

    private void setPermissionsLogic(){

        /*
        MultiplePermissionsListener dialogMultiplePermissionsListener =
                DialogOnAnyDeniedMultiplePermissionsListener.Builder
                        .withContext(getContext())
                        .withTitle("Permisos de ubicación")
                        .withMessage("Se necesitan los servicios de ubicación para el correcto funcionamiento de la aplicación")
                        .withButtonText(android.R.string.ok)
                        .withIcon(R.mipmap.ic_launcher)
                        .build();


        MultiplePermissionsListener snackbarMultiplePermissionsListener =
                SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                        .with(view, "Debes de aceptar los permisos de ubicación")
                        .withOpenSettingsButton("Abrir Ajustes")
                        .withCallback(new Snackbar.Callback() {
                            @Override
                            public void onShown(Snackbar snackbar) {}
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {}
                        })
                        .build();

        */


        MultiplePermissionsListener permissionsChecked = new MultiplePermissionsListener() {
            @Override public void onPermissionsChecked(MultiplePermissionsReport report) {

                if(report.isAnyPermissionPermanentlyDenied()){
                    viewTodosPermisos.setVisibility(View.VISIBLE);
                    Log.d("TAG","PERMANENTYLE DENIED");
                    return;
                }

                if(report.areAllPermissionsGranted()){
                    try {
                        viewTodosPermisos.setVisibility(View.GONE);
                        googleMap.setMyLocationEnabled(true);
                    }catch(SecurityException se){se.printStackTrace();}
                }
                else{
                    viewTodosPermisos.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        };

        MultiplePermissionsListener compositePermissionsListener =
                new CompositeMultiplePermissionsListener(
                        //snackbarMultiplePermissionsListener,
                        //dialogMultiplePermissionsListener,
                        permissionsChecked);

        dexterBuilder = Dexter.withActivity(getActivity()).withPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        ).withListener(compositePermissionsListener).onSameThread();
    }


    private void abrirAjustes(){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        if(getActivity()!=null) {
            Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        }
    }

    private void camToCentroid(ArrayList<LatLng> knots)  {
        double centroidX = 0, centroidY = 0;

        for(LatLng knot : knots) {
            centroidX += knot.latitude;
            centroidY += knot.longitude;
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(centroidX / knots.size(), centroidY / knots.size()),15));
    }

    private void clearItem(){
        itemNombre.setText(""); itemStatus.setText("");
        itemLetras.setText(""); itemPagos.setText("");
        itemDireccion.setText("");
        try {
            itemFachada.setImageDrawable(null);
        }catch(Exception e){}
    }

    private void checkExistence(final PuntoRutaWrapper puntoRutaWrapper){
        if(puntoRutaWrapper.getCliente()==null) {
            WS.readClientFirebase(puntoRutaWrapper.getPlan().getStCliente(), new WS.FirebaseObjectRetrievedListener() {
                @Override
                public void firebaseObjectRetrieved(Object_Firebase objectFirebase) {
                    Cliente_Firebase clienteFirebase = (Cliente_Firebase)objectFirebase;
                    Log.d(TAG, "clienteFirebase was NULL, not now");
                    puntoRutaWrapper.setCliente(clienteFirebase);
                    setItem(puntoRutaWrapper);
                }
            });
        }else{
            setItem(puntoRutaWrapper);
        }
    }

    private void setItem(PuntoRutaWrapper puntoRutaWrapper){
        try{
            lastSelected = puntoRutaWrapper;
            Cliente_Firebase clienteFirebase = puntoRutaWrapper.getCliente();
            itemNombre.setText(getString(
                R.string.format_fullname,
                clienteFirebase.getStNombre(),
                clienteFirebase.getStApellido()
            ));
            itemStatus.setText(getStatusLabel(clienteFirebase.getIntStatus()));
            itemLetras.setText(getString(
                R.string.detallecliente_format_letras,
                clienteFirebase.getStNombre().toUpperCase().substring(0,1),
                clienteFirebase.getStApellido().toUpperCase().substring(0,1)
            ));
            itemPagos.setText(getString(
                    R.string.format_cobro,
                    puntoRutaWrapper.getPlan().getPagosRealizados()+1));

            List<Direcciones> dirs = clienteFirebase.getDirecciones();
            for(Direcciones dir : dirs){
                Log.d(TAG, dir.getStCalleNum()+" "+dir.getStColonia());
            }
            Log.d(TAG, dirs.get(0).getStCalleNum()+" "+dirs.get(0).getStColonia());
            itemDireccion.setText(
                getString(
                    R.string.format_fulldireccion,
                    dirs.get(0).getStCalleNum(),
                    dirs.get(0).getStColonia(),
                    dirs.get(0).getStCP()
                ));
            Picasso.with(getActivity()).load(dirs.get(0).getLinkFachada()).into(itemFachada);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void showDetalleCobro(PuntoRutaWrapper puntoRutaWrapper){
            Intent intent = new Intent(getContext(), DetalleCobro.class);
            Bundle bundle = new Bundle();
            bundle.putString("clienteID", puntoRutaWrapper.getPlan().getStCliente());
            bundle.putString("planID", puntoRutaWrapper.getPlan().getStID());
            intent.putExtras(bundle);
            startActivity(intent);
    }

    private void showPopMenu(PuntoRutaWrapper puntoRutaWrapper, View viewClicked){

        PopupMenu popupMenu = new PopupMenu(getActivity(), viewClicked);

        int cont=0;
        for(Map<String,Object> eachEstatus : mapaEstatusVisita) {
            String nombre="";
            int id=-1;
            for (Map.Entry<String, Object> entry : eachEstatus.entrySet()) {
                Log.d(TAG, "entrey=" + entry.getKey() + ";" + entry.getValue()+"cont="+cont);
                if(entry.getKey().equals("nombre")){
                    nombre=entry.getValue().toString();
                }else if(entry.getKey().equals("idestatus")){
                    id=Integer.valueOf(entry.getValue().toString());
                }
            }
            popupMenu.getMenu().add(0,id,cont,nombre);
            cont++;

        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.d(TAG, "itemID="+item.getItemId()+" string="+item.getTitle());
                return true;
            }
        });
        popupMenu.show();
    }

    private String getStatusLabel(int status){
        switch (status){
            case Cliente_Firebase.STATUS_PROSPECTO:
                return "Prospecto";
            case Cliente_Firebase.STATUS_ENVERIFICACION:
                return "En Verificación";
            case Cliente_Firebase.STATUS_ACTIVO:
                return "Activo";
        }
        return "";
    }

    // ----------------------------------------------------- //
    // ---------------- MAP IMPLEMENTATIONS ---------------- //
    //------------------------------------------------------ //

    @Override
    public void onMapReady(GoogleMap googleMap_) {

        Log.d("MapFragment","MapReady");
        googleMap = googleMap_;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(19.441976, -98.943045),7));

        setPermissionsLogic();
        dexterBuilder.check();

        //After setup
        sheetBehaviorLista = BottomSheetBehavior.from(bottomsheetLayoutLista);
        sheetBehaviorLista.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                mostrarOcultarLista.setText(getString(
                        newState==BottomSheetBehavior.STATE_EXPANDED ?
                        R.string.cobrosfrag_ocultarlista : R.string.cobrosfrag_mostrarlista));
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) { }
        });

        headerBottomSheetLista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sheetBehaviorLista.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                    if(sheetBehaviorItem.getState()!=BottomSheetBehavior.STATE_EXPANDED) {
                        sheetBehaviorLista.setState(BottomSheetBehavior.STATE_EXPANDED);
                    }
                } else {
                    sheetBehaviorLista.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            }
        });

        sheetBehaviorItem.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if(newState==BottomSheetBehavior.STATE_HIDDEN){
                    clearItem();
                    lastSelected=null;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) { }
        });

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                sheetBehaviorItem.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });

        //WS.readRutasAsignadas(CobrosFrag.this);

        WS.readRutasPorEmpleado(CobrosFrag.this);

        WS.readRutasPorEmpleado(CobrosFrag.this, getActivity(), true);
    }

    // --------------------------------------------------------------- //
    // ---------------- COMPONENTCLICK IMPLEMENTATION ---------------- //
    //---------------------------------------------------------------- //

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.cobros_abrir_ajustes:{
                try{abrirAjustes();}catch(Exception e){e.printStackTrace();}
                break;
            }

            case R.id.item_perfilcobrador_pagado:{
                if(lastSelected!=null){
                    showDetalleCobro(lastSelected);
                }
                break;
            }

            case R.id.item_perfilcobrador_fecha:{
                if(lastSelected!=null){
                    showPopMenu(lastSelected, view);

                }
            }
        }
    }

    // --------------------------------------------------------------- //
    // ---------------- COMPONENTCLICK IMPLEMENTATION ---------------- //
    //---------------------------------------------------------------- //

    @Override
    public void willRemoveItem(int position) {
        mDataset.get(position).getMarker().setVisible(false);
    }


    // --------------------------------------------------------------- //
    // ---------------- COMPONENTCLICK IMPLEMENTATION ---------------- //
    //---------------------------------------------------------------- //

    @Override
    public void onComponentClikListener(int ID, int position, View viewClicked, PopupMenu menu) {
        switch (ID){
            case R.id.item_perfilcobrador_pagado:{
                showDetalleCobro(mDataset.get(position));
                break;
            }

            case R.id.item_perfilcobrador_fecha:{
                showPopMenu(mDataset.get(position), viewClicked);
                break;
            }
        }
    }

    private void drawPosOnMap(){
        googleMap.clear();
    }

    // --------------------------------------------------------- //
    // ---------------- FIREBASE IMPLEMENTATION ---------------- //
    //---------------------------------------------------------- //

    private boolean checarAparecePorStatus(Plan_Firebase planFirebase){
        return true;
    }

    private boolean checarAparecePorInsuficienciaSaldo(Plan_Firebase planFirebase){

        final String TAG = "checarAparece";

        final int SEMANAL = 1;
        final int QUINCENAL = 2;
        final int MENSUAL = 3;

        long createdAt = planFirebase.getCreatedAt();
        int frecuenciaID = planFirebase.getFrecuenciaPagoID();
        int numeroPagosARealizar = planFirebase.getNumeroDePagosARealizar();
        FrecuenciaPago_Firebase frecuenciaPagoFirebase = frecuenciasPagoFirebase.getFrecuenciaByID(planFirebase.getFrecuenciaPagoID());
        FinanciamientoOBJ_Firebase financiamientoObjFirebase = financiamientoFirebase.getFinanciamientoByID(planFirebase.getFinanciamientoID());

        Calendar cal = GregorianCalendar.getInstance();
        cal.setTimeInMillis(createdAt*1000);
        cal.set(Calendar.HOUR,17);cal.set(Calendar.MINUTE,00);cal.set(Calendar.SECOND,00);

        Calendar calHoy = GregorianCalendar.getInstance();
        calHoy.setTimeInMillis(System.currentTimeMillis());
        calHoy.set(Calendar.HOUR,17);calHoy.set(Calendar.MINUTE,00);calHoy.set(Calendar.SECOND,00);

        //Log.d(TAG, "antes frecuenciaID="+frecuenciaID+" numeroPagosARealizar="+numeroPagosARealizar);

        //Buscamos si tiene un dia de visitas customizado
        if(planFirebase.getDiaVisitas()!=0 && planFirebase.getFrecuenciaVisitas()!=0){
            //SI es asi, internamente cambiamos la frecuencia
            frecuenciaID=planFirebase.getFrecuenciaVisitas();

            //Y seteamos internamente el nuevo numero de pagos a realizar con la periodicidad de visitasProgramadas
            numeroPagosARealizar = Math.round(  financiamientoObjFirebase.getAnios()*frecuenciasPagoFirebase.getFrecuenciaByID(frecuenciaID).getPagosAlAnio() );
        }

        //Log.d(TAG, "ahora frecuenciaID="+frecuenciaID+" numeroPagosARealizar="+numeroPagosARealizar);

        //Primero obtenemos cuantos pagos ya lleva en teoria hechos en relacion al saldo ya pagado NO AL NUMERO DE PAGOS
        int periodosYaPagados = Math.round(numeroPagosARealizar - ((planFirebase.getSaldo() * numeroPagosARealizar) / planFirebase.getTotalAPagar()));

        //Buscamos si tiene un dia de visitas customizado
        if(planFirebase.getDiaVisitas()!=0 && planFirebase.getFrecuenciaVisitas()!=0){
            //Calculamos los pagos que lleva y hasta donde le cubren
            Calendar fechaQueTieneCubierta = GregorianCalendar.getInstance();
            fechaQueTieneCubierta.setTimeInMillis(createdAt*1000);
            fechaQueTieneCubierta.set(Calendar.HOUR,17);fechaQueTieneCubierta.set(Calendar.MINUTE,00);fechaQueTieneCubierta.set(Calendar.SECOND,00);
            switch(frecuenciaID){
                case SEMANAL:{
                    fechaQueTieneCubierta.add(Calendar.DAY_OF_MONTH,7*periodosYaPagados);
                    break;
                }

                case QUINCENAL:{
                    fechaQueTieneCubierta.add(Calendar.DAY_OF_MONTH,14*periodosYaPagados);
                    break;
                }

                case MENSUAL:{
                    fechaQueTieneCubierta.add(Calendar.MONTH,1*periodosYaPagados);
                    break;
                }
            }


            //si para el dia de hoy esta cubierta con sus pagos, no debe aparecer la tarjeta, de lo contrario si
            if(calHoy.before(fechaQueTieneCubierta)){
                return false;
            }else{
                return true;
            }

        }

        Log.d(TAG, "cadaPeriodoEnPesos=");

        //Contamos los periodos que deberia de llevar
        int periodosDeberiaLlevar=0;
        do{
            switch (frecuenciaID) {
                case SEMANAL: {
                    cal.add(Calendar.DAY_OF_MONTH,7);
                    periodosDeberiaLlevar++;
                    break;
                }

                case QUINCENAL: {
                    cal.add(Calendar.DAY_OF_MONTH,14);
                    periodosDeberiaLlevar++;
                    break;
                }

                case MENSUAL: {
                    cal.add(Calendar.MONTH,1);
                    periodosDeberiaLlevar++;
                    break;
                }
            }
        }while(cal.before(calHoy));

        Log.d(TAG, "deberiaLlevar["+periodosDeberiaLlevar+"] lleva["+periodosYaPagados+"]");

        int difPeriodos = periodosDeberiaLlevar-periodosYaPagados;
        if((difPeriodos>0 && difPeriodos<=2)){
            return true;
        }else{
            return false;
        }

        //23/Jun/2018 = 1529766610
        //24/Jun/2018 = 1529853010
        //25/Jun/2018 = 1529939410
        //30/Jun/2018 = 1530371410

    }

    @Override
    public void firebaseObjectRetrieved(Object_Firebase obj) {
        if(!initialMapSet){
            String empleado = WS.getCurrentUser().getEmail().split("@")[0].replace(".","");

            tituloRutas.setText(empleado);
            subtituloRutas.setText("Ruta");
            mostrarOcultarLista.setText(getString(R.string.cobrosfrag_mostrarlista));
            mostrarOcultarLista.setVisibility(View.VISIBLE);

            ArrayList<LatLng> latLngs = new ArrayList<>();

            googleMap.clear();
        }

        if( (((Plan_Firebase)obj).getStatus()==10 || ((Plan_Firebase)obj).getStatus()==1 || ((Plan_Firebase)obj).getStatus()==2) && checarAparecePorInsuficienciaSaldo((Plan_Firebase)obj)) {

            Direcciones dir = ((Plan_Firebase) obj).getDirecciones().get(0);

            Log.d(TAG, "latLon[" + dir.getLat() + "," + dir.getLon() + "]");

            //polyOptions.add(new LatLng(punto.getLat(), punto.getLon()));

            //latLngs.add(new LatLng(dir.getLat(), dir.getLon()));

            Marker m = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(dir.getLat(), dir.getLon()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pinpng)));

            mDataset.add(new PuntoRutaWrapper(m, ((Plan_Firebase) obj)));

            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    for (PuntoRutaWrapper puntoRutaWrapper : mDataset) {
                        Log.d(TAG, "L[" + puntoRutaWrapper.getMarker().getId() + "]" + " a[" + marker.getId() + "]");
                        if (puntoRutaWrapper.getMarker().getId().compareTo(marker.getId()) == 0) {
                            sheetBehaviorItem.setState(BottomSheetBehavior.STATE_EXPANDED);
                            checkExistence(puntoRutaWrapper);
                        }
                    }
                    return false;
                }
            });

            try {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (PuntoRutaWrapper pRW : mDataset) {
                    builder.include(pRW.getMarker().getPosition());
                }
                LatLngBounds bounds = builder.build();
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 11);
                googleMap.moveCamera(cu);
                googleMap.setPadding(0, getResources().getDrawable(R.drawable.ic_pinpng).getIntrinsicHeight(), 0, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        progressLista.setVisibility(View.GONE);

        mAdapter.notifyDataSetChanged();

        if(!initialMapSet)
        headerBottomSheetLista.callOnClick();

        initialMapSet=true;
    }

    @Override
    public void firebaseCompleted(ArrayList<Object_Firebase> arrayList) {

        Log.d(TAG, "llegaronNumPlanes="+arrayList.size());


        String empleado = WS.getCurrentUser().getEmail().split("@")[0].replace(".","");

        tituloRutas.setText(empleado);
        subtituloRutas.setText("Ruta");
        mostrarOcultarLista.setText(getString(R.string.cobrosfrag_mostrarlista));
        mostrarOcultarLista.setVisibility(View.VISIBLE);


        ArrayList<LatLng> latLngs = new ArrayList<>();

        googleMap.clear();
        //PolygonOptions polyOptions = new PolygonOptions();
        for(Object_Firebase obj : arrayList){
            Direcciones dir = ((Plan_Firebase)obj).getDirecciones().get(0);

            Log.d(TAG,"latLon["+dir.getLat()+","+dir.getLon()+"]");

            //polyOptions.add(new LatLng(punto.getLat(), punto.getLon()));

            latLngs.add(new LatLng(dir.getLat(), dir.getLon()));

            Marker m = googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(dir.getLat(), dir.getLon()))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pinpng)));

            mDataset.add(new PuntoRutaWrapper(m,((Plan_Firebase)obj)));

            googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    for(PuntoRutaWrapper puntoRutaWrapper : mDataset){
                        Log.d(TAG, "L["+puntoRutaWrapper.getMarker().getId()+"]"+" a["+marker.getId()+"]");
                        if(puntoRutaWrapper.getMarker().getId().compareTo(marker.getId())==0){
                            sheetBehaviorItem.setState(BottomSheetBehavior.STATE_EXPANDED);
                            checkExistence(puntoRutaWrapper);
                        }
                    }
                    return false;
                }
            });

        }

        //camToCentroid(latLngs);

        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (PuntoRutaWrapper pRW : mDataset) {
                builder.include(pRW.getMarker().getPosition());
            }
            LatLngBounds bounds = builder.build();
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 11);
            googleMap.moveCamera(cu);
            googleMap.setPadding(0, getResources().getDrawable(R.drawable.ic_pinpng).getIntrinsicHeight(), 0, 0);
        }catch(Exception e){e.printStackTrace();}


        /*
        if(getActivity()!=null) {
            polyOptions.strokeColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryDarkT))
                    .fillColor(ContextCompat.getColor(getActivity(), R.color.colorPrimaryT));
            googleMap.addPolygon(polyOptions);
            camToCentroid(latLngs);
        }
        */

        progressLista.setVisibility(View.GONE);

        mAdapter.notifyDataSetChanged();
        headerBottomSheetLista.callOnClick();
    }

}
