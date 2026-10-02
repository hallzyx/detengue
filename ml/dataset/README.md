# Dataset
Formato YOLO: `images/{train,val,test}` y `labels/{train,val,test}`. Clases en `android/models/detengue_labels.txt`.
Reglas: fotos propias o con consentimiento, sin rostros ni datos personales legibles; variedad de luz, distancia y fondos. Valida con `python -m ml.validate_dataset ml/dataset`.

## MVP sin fotos propias (datasets públicos)
Ver `ml/MVP_COLAB.md`. `python -m ml.import_external` convierte exports YOLO de Roboflow a las clases de
`detengue_labels.txt` y anota fuente y licencia en `ml/dataset/SOURCES.md`.
