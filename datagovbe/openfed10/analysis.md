# Analysis

## Modules

- Contact
- Date time
- Date time range
- Facets
- Field group
- HAL
- HTTP Basic Authentication
- Geofield
- JSON:API
- Metatag: Dublin Core
- Metatag: Dublin Core Advanced
- Openfed multilingual
- Openfed social links
- Optional end date
- Path
- RESTful Web Services
- RDF
- Search API Autocomplete
- Search API Solr
- Search API Solr Admin
- Search API Solr Autocomplete
- Serialization
- Simple XML Sitemap
- Simple XML Sitemap Search Engines

## Taxonomies
### Taxonomy: Application Types

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

### Taxonomy: Contact Types

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

### Taxonomy: Data Categories

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

### Taxonomy: File Types

Language-independent

| Field | Type | Required |
| --- | --- | --- |
| Title | String | yes |
| URI | Link | yes |

### Taxonomy: Geographies

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes  | yes |
| Bounding box | Geofield (WKT) | yes | no |
| URI | Link | yes | no |

### Taxonomy: Licenses

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

### Taxonomy: Organizations

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

### Taxonomy: Update Frequencies

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

## Conten types

### Content type: Application

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |
| Image | Image | no | no | no |
| Website | Link | yes | yes | no |
| Category | Taxonomy: Data Categories | -- | yes |

### Content type: Dataset

| Field | Type | Required | Translatable | Multiple | Display |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no | text |
| Description | Text field | yes | yes | no | text |
| URI | Link | yes | no | no | hidden |
| Update frequancy | Taxonomy: Update Frequencies | no | -- | no | text |
| Last modified | Timestamp | no | no | no | date |
| From / till | Date range | no | no | no | date range |
| Author | String | yes | yes | yes | text |
| Publisher | Taxonomy: Organizations | -- | no | link |
| Contact e-mail | E-mail | no | yes | no | link |
| Contact form | Link | no | yes | no | link |
| Geography | Taxonomy: Geographies | yes | -- | yes |
| Category | Taxonomy: Data Categories | yes | -- | yes | text |
| License | Taxonomy: Licenses | yes | -- | yes | text |
| Format | Taxonomy: File types | yes | -- | yes | text |
| Web page | Link | no | yes, no sync | yes | link |
| Download URL | Link | no | yes, no sync | yes | link |
| Service URL | Link | no | yes, no sync | yes | link |
| Keyword | String | yes, no sync | yes | hidden |

### Content type: News

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |
| Image | Image | no | no | no |
| Website | Link | no | yes | no |

### Contact form

| Field | Type | Required | Display |
| --- | --- | --- | --- |
| Your name | String | yes | single line |
| Your e-mail | Email | yes | single line |
| Category | Taxonomy: Contact Types | yes | select list |
| Subject | String | yes | string |
| Message | Text field | yes | multi-line text field |
| Agree policy | Boolean | yes | checkbox |
