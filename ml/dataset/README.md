# Dataset
Formato YOLO: `images/{train,val,test}` y `labels/{train,val,test}`. Clases en `android/models/detengue_labels.txt`.
Reglas: fotos propias o con consentimiento, sin rostros ni datos personales legibles; variedad de luz, distancia y fondos. Valida con `python -m ml.validate_dataset ml/dataset`.
