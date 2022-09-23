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

## Taxonomy: Application Types

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

## Taxonomy: Contact Types

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

## Taxonomy: Data Categories

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |

## Taxonomy: File Types

Language-independent

| Field | Type | Required |
| --- | --- | --- |
| Title | String | yes |
| URI | Link | yes |

## Taxonomy: Geographies

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes  | yes |
| Bounding box | Geofield (WKT) | yes | no |
| URI | Link | yes | no |

## Taxonomy: Licenses

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

## Taxonomy: Organizations

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

## Taxonomy: Update Frequencies

| Field | Type | Required | Translatable |
| --- | --- | --- | --- |
| Title | String | yes | yes |
| Abbreviation | String | no | yes |
| URI | Link | yes | no |

## Content type: Application

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |
| Image | Image | no | no | no |
| Website | Link | yes | yes | no |
| Category | Taxonomy: Data Categories | -- | yes |

## Content type: Dataset

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |
| Update frequancy | Taxonomy: Update Frequencies | no | -- | no |
| Last modified | Timestamp | no | no | no |
| From / till | Date range | no | no | no |
| Author | String | yes | yes | yes |
| Publisher | Taxonomy: Organizations | -- | no |
| Contact e-mail | E-mail | no | yes | no |
| Contact form | Link | no | yes | no |
| Geography | Taxonomy: Geographies | yes | -- | yes |
| Category | Taxonomy: Data Categories | yes | -- | yes |
| License | Taxonomy: Licenses | yes | -- | yes |
| Format | Taxonomy: File types | yes | -- | yes |
| Web page | Link | no | yes, no sync | yes |
| Download URL | Link | no | yes, no sync | yes |
| Service URL | Link | no | yes, no sync | yes |
| Keyword | String | yes, no sync | yes |


## Content type: News

| Field | Type | Required | Translatable | Multiple |
| --- | --- | --- | --- | --- |
| Title | String | yes | yes | no |
| Description | Text field | yes | yes | no |
| Image | Image | no | no | no |
| Website | Link | no | yes | no |

## Contact form

| Field | Type | Required |
| --- | --- | --- |
| Your name | String | yes |
| Your e-mail | Email | yes |
| Category | Taxonomy: Contact Types | yes |
| Subject | String | yes |
| Message | Text field | yes |
| Agree policy | Boolean | yes |
