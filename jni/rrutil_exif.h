#ifndef __RRUTIL_EXIF_H__
#define __RRUTIL_EXIF_H__

#ifdef __cplusplus
extern "C" {
#endif

#include <jpeg-marker.h>

#include <exif-data.h>
#include <exif-log.h>

typedef ExifData * JPEGContentAPP1;

typedef struct _JPEGContentGeneric JPEGContentGeneric;
struct _JPEGContentGeneric
{
	unsigned char *data;
	unsigned int size;
};

typedef union _JPEGContent JPEGContent;
union _JPEGContent
{
	JPEGContentGeneric generic;
	JPEGContentAPP1    app1;
};

typedef struct _JPEGSection JPEGSection;
struct _JPEGSection
{
	JPEGMarker marker;
	JPEGContent content;
};

typedef struct _JPEGData        JPEGData;
typedef struct _JPEGDataPrivate JPEGDataPrivate;

struct _JPEGData
{
	JPEGSection *sections;
	unsigned int count;

	unsigned char *data;
	unsigned int size;

	JPEGDataPrivate *priv;
};

JPEGData *jpeg_data_new           (void);
JPEGData *jpeg_data_new_from_file (const char *path);
JPEGData *jpeg_data_new_from_data (const unsigned char *data,
				   unsigned int size);

void      jpeg_data_ref   (JPEGData *data);
void      jpeg_data_unref (JPEGData *data);
void      jpeg_data_free  (JPEGData *data);

void      jpeg_data_load_data     (JPEGData *data, const unsigned char *d,
				   unsigned int size);
void      jpeg_data_save_data     (JPEGData *data, unsigned char **d,
				   unsigned int *size);

void      jpeg_data_load_file     (JPEGData *data, const char *path);
int       jpeg_data_save_file     (JPEGData *data, const char *path);

void      jpeg_data_set_exif_data (JPEGData *data, ExifData *exif_data);
ExifData *jpeg_data_get_exif_data (JPEGData *data);

void      jpeg_data_append_section (JPEGData *data);

#ifdef __cplusplus
}
#endif


#endif
